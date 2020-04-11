package oscrabble.server;

import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.HashBag;
import org.apache.commons.collections4.bag.TreeBag;
import org.apache.log4j.Logger;
import oscrabble.*;
import oscrabble.action.Action;
import oscrabble.action.Exchange;
import oscrabble.action.PlayTiles;
import oscrabble.action.SkipTurn;
import oscrabble.client.SwingPlayer;
import oscrabble.configuration.Parameter;
import oscrabble.configuration.PropertyUtils;
import oscrabble.dictionary.Dictionary;
import oscrabble.player.AbstractPlayer;
import oscrabble.player.BruteForceMethod;

import java.io.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Game implements IGame
{
	/**
	 * Resource Bundle
	 */
	public final static ResourceBundle MESSAGES = ResourceBundle.getBundle("Messages", new Locale("fr_FR"));

	private final static Logger LOGGER = Logger.getLogger(Game.class);

	public final static int RACK_SIZE = 7;
	private static final String SCRABBLE_MESSAGE = "Scrabble!";
	private static final String PLAYER_PREFIX = "player.";
	private static final String KEY_METHOD = "method";
	public static final File DEFAULT_PROPERTIES_FILE = new File(System.getProperty("user.home"), ".scrabble.properties");

	/**
	 *  Seed initially used to create the random generator.
	 */
	final long randomSeed;

	/**
	 * Delay (in seconds) before changing the state from ENDING to ENDED
	 */
	int delayBeforeEnds = 3;

	private final Map<AbstractPlayer, PlayerInfo> players = new LinkedHashMap<>();

	/**
	 * List of the users, the first to play at head
	 */
	private final LinkedList<AbstractPlayer> toPlay = new LinkedList<>();
	private final Grid grid;
	private final Random random;
	private CountDownLatch waitingForPlay;
	private final LinkedList<Tile> bag = new LinkedList<>();
	private final Dictionary dictionary;

	final List<GameListener> listeners = new ArrayList<>();

	/**
	 * State of the game
	 */
	private State state;

	/**
	 * Configuration of the game
	 */
	private final Configuration configuration;

	/**
	 * Plays
	 */
	final LinkedList<Play> plays = new LinkedList<>();

	/**
	 * History of the game, a line played move (even if it was an error).
	 */
	private final List<HistoryEntry> history = Collections.synchronizedList(new LinkedList<>());

	/**
	 * Synchronize: to by synchronized by calls which change the state of the game
	 */
	final Object changing = new Object();

	/**
	 * File to save and read the game configuration
	 */
	private final File propertyFile;

	public Game(final File propertyFile) throws ConfigurationException
	{
		if (propertyFile == null)
		{
			throw new IllegalArgumentException("Property file cannot be null");
		}

		if (!propertyFile.exists())
		{
			throw new AssertionError("Property file does not exist");
		}

		this.propertyFile = propertyFile;
		final Properties properties = new Properties();
		try (FileReader reader = new FileReader(propertyFile))
		{
			properties.load(reader);
		}
		catch (final IOException ex)
		{
			final ConfigurationException configurationException = new ConfigurationException("Cannot read the configuration file: " + ex.toString(), ex);
			LOGGER.error(configurationException);
			throw configurationException;
		}

		this.configuration = new Configuration();
		this.configuration.loadProperties(properties);
		try
		{
			this.dictionary = Dictionary.getDictionary(this.configuration.dictionary);
		}
		catch (IllegalArgumentException e)
		{
			throw new ConfigurationException("Not known language: " + this.configuration.dictionary);
		}
		this.configuration.addChangeListener(evt -> saveConfiguration());

		//
		// Players
		//

		final Pattern keyPart = Pattern.compile("([^.]*)");
		final Set<String> playerNames = new HashSet<>();
		PropertyUtils.getSubProperties(properties, "player").stringPropertyNames().forEach(k ->
		{
			Matcher m = keyPart.matcher(k);
			if (m.find())
			{
				playerNames.add(m.group(1));
			}
		});

		for (final String name : playerNames)
		{
			final Properties playerProps = PropertyUtils.getSubProperties(properties, PLAYER_PREFIX + name);
			final AbstractPlayer player;
			final String methodName = playerProps.getProperty(KEY_METHOD);
			switch (PlayerType.valueOf(methodName.toUpperCase()))
			{
				case SWING:
					player = new SwingPlayer(name);
					break;
				case BRUTE_FORCE:
					player = new BruteForceMethod(this.dictionary).new Player(name);
					((BruteForceMethod.Player) player).loadConfiguration(playerProps);
					break;
				default:
					throw new ConfigurationException("Unknown method: " + methodName);
			}
			this.addPlayer(player);
			final oscrabble.configuration.Configuration playerConfig = player.getConfiguration();
			if (playerConfig != null)
			{
				playerConfig.addChangeListener(evt -> saveConfiguration());
			}
		}

		this.grid = new Grid(this.dictionary);
		this.random = new Random();
		this.randomSeed = this.random.nextLong();
		this.random.setSeed(this.randomSeed);
		this.waitingForPlay = new CountDownLatch(1);

		setState(State.BEFORE_START);
		LOGGER.info("Created game with random seed " + this.random);
	}

	/**
	 * Constructor for test purposes a game without player.
	 *
	 * @param dictionary dictionary
	 * @param randomSeed seed to initialize the random generator
	 */
	public Game(final Dictionary dictionary, final long randomSeed)
	{
		this.randomSeed = randomSeed;
		this.random = new Random(randomSeed);
		this.dictionary = dictionary;
		this.grid = new Grid(dictionary);
		this.propertyFile = null;
		this.configuration = null;
	}

	/**
	 * Constructor for test purposes.
	 *
	 * @see #Game(Dictionary, long)
	 * @param dictionary dictionary
	 */
	public Game(final Dictionary dictionary)
	{
		this(dictionary, new Random().nextLong());
	}

	/**
	 * Save the configuration of this game
	 */
	private void saveConfiguration()
	{
		saveConfiguration(this.propertyFile, this.configuration, this.players.values());
	}

	@Override
	public void setState(final State state)
	{
		if (this.state != state)
		{
			this.state = state;
			this.listeners.forEach( l -> l.onGameStateChanged());
		}
	}

	@Override
	public synchronized void addPlayer(final AbstractPlayer newPlayer)
	{
		final PlayerInfo info = new PlayerInfo();
		info.player = newPlayer;
		info.key = UUID.randomUUID();
		newPlayer.setPlayerKey(info.key);
		info.rack = new Rack();
		info.incomingEventQueue = new LinkedBlockingDeque<>();
		this.players.put(newPlayer, info);
		this.listeners.add(newPlayer);
		newPlayer.setGame(this);
	}

	@Override
	public void addListener(final GameListener listener)
	{
		this.listeners.add(listener);
	}

	@Override
	public synchronized int play(final UUID clientKey, final Play play, final Action action) throws ScrabbleException.NotInTurn, ScrabbleException.InvalidSecretException
	{
		synchronized (this.changing)
		{
			if (this.plays.isEmpty() || this.plays.getLast() != play)
			{
				throw new ScrabbleException.NotInTurn(play.player);
			}

			assert !play.isDone();
			final AbstractPlayer player = play.player;
			final PlayerInfo playerInfo = this.players.get(player);
			checkKey(player, clientKey);

			LOGGER.info(player.getName() + " plays " + action);
			int score = 0;
			boolean actionRejected = false;
			Set<Tile> drawn = null;
			Grid.MoveMetaInformation moveMI = null;
			boolean done = false;
			try
			{
				final ArrayList<String> messages = new ArrayList<>(5);

				if (action instanceof PlayTiles)
				{
					final PlayTiles playTiles = (PlayTiles) action;

					// check possibility
					moveMI = this.grid.getMetaInformation(playTiles);
					final TreeBag<Character> rackLetters = new TreeBag<>();
					playerInfo.rack.forEach(stone -> {
						if (!stone.isJoker())
						{
							rackLetters.add(stone.getChar(), 1);
						}
					});

					final Bag<Character> requiredLetters = moveMI.getRequiredLetters();
					final Bag<Character> missingLetters = new HashBag<>(requiredLetters);
					missingLetters.removeAll(rackLetters);
					if (missingLetters.size() > playerInfo.rack.countJoker())
					{
						throw new ScrabbleException.ForbiddenPlayException(MessageFormat.format(MESSAGES.getString("html.rack.with.0.br.has.not.the.required.stones.1"), rackLetters, requiredLetters));
					}

					// check touch
					if (this.grid.isEmpty())
					{
						if (playTiles.word.length() < 2)
						{
							throw new ScrabbleException.ForbiddenPlayException(MESSAGES.getString("first.word.must.have.at.least.two.letters"));
						}
					}
					else if (moveMI.crosswords.isEmpty() && requiredLetters.size() == playTiles.word.length())
					{
						throw new ScrabbleException.ForbiddenPlayException(MESSAGES.getString("new.word.must.touch.another.one"));
					}

					// check dictionary
					final Set<String> toTest = new LinkedHashSet<>();
					toTest.add(playTiles.word);
					toTest.addAll(moveMI.crosswords);
					for (final String crossword : toTest)
					{
						if (!this.dictionary.containUpperCaseWord(crossword.toUpperCase()))
						{
							final String details = MessageFormat.format(MESSAGES.getString("word.0.is.not.allowed"), crossword);
							throw new ScrabbleException.ForbiddenPlayException(details);
						}
					}

					if (this.grid.isEmpty())
					{
						final Grid.Square center = this.grid.getCenter();
						if (!playTiles.getSquares().containsKey(center))
						{
							throw new ScrabbleException.ForbiddenPlayException(MESSAGES.getString("the.first.word.must.be.on.the.center.square"));
						}
					}

					Grid.Square square = playTiles.startSquare;
					for (int i = 0; i < playTiles.word.length(); i++)
					{
						final char c = playTiles.word.charAt(i);
						if (square.isEmpty())
						{
							final Tile tile =
									playerInfo.rack.removeStones(
											Collections.singletonList(playTiles.isPlayedByBlank(i) ? ' ' : c)
									).get(0);
							if (tile.isJoker())
							{
								tile.setCharacter(c);
							}
							this.grid.set(square, tile);
							tile.setSettingAction(action);
						}
						else
						{
							assert square.tile.getChar() == c; //  sollte schon oben getestet sein.
						}
						square = square.getFollowing(playTiles.getDirection());
					}

					score = moveMI.getScore();
					if (moveMI.isScrabble)
					{
						messages.add(SCRABBLE_MESSAGE);
					}
					playerInfo.score += score;
					LOGGER.info(MessageFormat.format(MESSAGES.getString("0.plays.1.for.2.points"), playerInfo.getName(), playTiles.getNotation(), score));
				}
				else if (action instanceof Exchange)
				{
					if (getNumberTilesInBag() < getRequiredTilesInBagForExchange())
					{
						throw new ScrabbleException.ForbiddenPlayException(MESSAGES.getString("not.enough.tiles.in.bag.for.exchange"));
					}

					final Exchange exchange = (Exchange) action;
					final List<Tile> stones1 = playerInfo.rack.removeStones(exchange.getChars());
					this.bag.addAll(stones1);
					Collections.shuffle(this.bag, this.random);
					moveMI = null;
					LOGGER.info(playerInfo.getName() + " exchanges " + exchange.getChars().size() + " stones");
				}
				else if (action instanceof SkipTurn)
				{
					LOGGER.info(playerInfo.getName() + " skips its turn");
					this.dispatchMessage(MessageFormat.format(MESSAGES.getString("0.skips.its.turn"), playerInfo.getName()));
				}
				else
				{
					throw new AssertionError("Command not treated: " + action);
				}

				playerInfo.isLastPlayError = false;
				drawn = refillRack(playerInfo.player);
				messages.forEach(message -> dispatchMessage(message));

				LOGGER.debug("Grid after play move nr #" + play.uuid + ":\n" + this.grid.asASCIIArt());
				actionRejected = false;
				done = true;
				return score;
			}
			catch (final ScrabbleException e)
			{
				LOGGER.info("Refuse play: " + action + ". Cause: " + e);
				actionRejected = true;
				playerInfo.player.onDispatchMessage(e.getLocalizedMessage());
				if (this.configuration.retryAccepted /* TODO: several places for blanks || e.acceptRetry()*/)
				{
					player.getIncomingEventQueue().add(p -> p.onPlayRequired(play));
				}
				else
				{
					dispatch(listener -> listener.afterRejectedAction(playerInfo.player, action));
					playerInfo.isLastPlayError = true;
					done = true;
				}
				return 0;
			}
			finally
			{
				if (done)
				{
					playerInfo.lastAction = action;
					dispatch(toInform -> toInform.afterPlay(play));
					final HistoryEntry historyEntry = new HistoryEntry(play, actionRejected, score, drawn, moveMI);
					this.history.add(historyEntry);
					this.toPlay.pop();
					this.toPlay.add(player);

					if (playerInfo.rack.isEmpty())
					{
						endGame(playerInfo, historyEntry);
					}
					else if (action == SkipTurn.SINGLETON)
					{
						// Quit if nobody can play
						final AtomicBoolean canPlay = new AtomicBoolean(false);
						this.players.forEach((p, mi) -> {
							if (mi.lastAction != SkipTurn.SINGLETON) canPlay.set(true);
						});
						if (!canPlay.get())
						{
							endGame(null, historyEntry);
						}
					}
					play.action = action;
					this.waitingForPlay.countDown();
				}
			}
		}
	}

	public synchronized void rollbackLastMove(final AbstractPlayer caller, final UUID callerKey) throws ScrabbleException
	{
		synchronized (this.changing)
		{
			LOGGER.info("Rollback last move on demand of " + caller);
			if (this.history.isEmpty())
			{
				throw new ScrabbleException.InvalidStateException(MESSAGES.getString("no.move.played.for.the.time"));
			}
			final HistoryEntry historyEntry = this.history.remove(this.history.size() - 1);
			LOGGER.info("Rollback " + historyEntry.formatAsString());

			final AbstractPlayer rollbackedPlayer = historyEntry.getPlayer();
			final PlayerInfo playerInfo = this.players.get(rollbackedPlayer);
			playerInfo.rack.removeAll(historyEntry.drawn);
			historyEntry.metaInformation.getFilledSquares().forEach(
					filled -> playerInfo.rack.add(filled.tile)
			);
			this.grid.remove(historyEntry.metaInformation);
			this.players.forEach( (p,info) -> info.score -= historyEntry.scores.getOrDefault(p, 0));
			assert this.toPlay.peekLast() == rollbackedPlayer;

			if (this.state == State.STARTED)
			{
				this.plays.removeLast();
			}
			this.plays.removeLast();

			this.toPlay.removeLast();
			this.toPlay.addFirst(rollbackedPlayer);
			dispatch(toInform -> toInform.afterRollback());

			setState(State.STARTED);

			this.waitingForPlay.countDown();
			this.changing.notify();
		}

	}

	@Override
	public void playerConfigHasChanged(final AbstractPlayer player, final UUID playerKey)
	{
		saveConfiguration();
	}

	@Override
	public int getNumberTilesInBag()
	{
		return this.bag.size();
	}

	@Override
	public int getRequiredTilesInBagForExchange()
	{
		// This limit is 7 for French and German Scrabble, could be another one of other languages.
		// see https://www.fisf.net/scrabble/decouverte-du-scrabble/formules-de-jeu.html
		// and Turnierspielordnung of Scrabble Deutschland e.V.
		return 7;
	}

	@Override
	public synchronized AbstractPlayer getPlayerToPlay()
	{
		return this.toPlay.getFirst();
	}

	/**
	 * Ends the game.
	 *
	 * @param firstEndingPlayer player which has first emptied its rack, or {@code null} if nobody has cleared it.
	 */
	private synchronized void endGame(final PlayerInfo firstEndingPlayer, final HistoryEntry historyEntry)
	{
		LOGGER.info("Games ends. Player which have clear its rack: " + (firstEndingPlayer == null ? null : firstEndingPlayer.getName()));
		final StringBuffer message = new StringBuffer();
		if (firstEndingPlayer == null)
		{
			message.append(MESSAGES.getString("game.ends.without.any.player.have.cleared.its.rack"));
		}
		else
		{
			message.append(MessageFormat.format(MESSAGES.getString("0.has.cleared.its.rack"), firstEndingPlayer.getName())).append('\n');
		}
		this.players.forEach(
				(player, info) ->
				{
					if (info != firstEndingPlayer)
					{
						int gift = 0;
						for (final Tile tile : info.rack)
						{
							gift += tile.getPoints();
						}
						info.score -= gift;
						historyEntry.scores.put(player, -gift);
						if (firstEndingPlayer != null)
						{
							firstEndingPlayer.score += gift;
							historyEntry.scores.put(firstEndingPlayer.player, historyEntry.scores.get(firstEndingPlayer.player) + gift);
						}
						message.append(MessageFormat.format(MESSAGES.getString("0.gives.1.points"), info.getName(), gift)).append("\n");
					}
				});

		dispatch(c -> c.onDispatchMessage(message.toString()));
		setState(State.ENDED);
	}


	/**
	 * Send a message to each listener.
	 *
	 * @param message message to dispatch
	 */
	private void dispatchMessage(final String message)
	{
		dispatch(l -> l.onDispatchMessage(message));
	}

	@Override
	public List<IPlayerInfo> getPlayers()
	{
		return new ArrayList<>(this.players.values());
	}

	@Override
	public Iterable<HistoryEntry> getHistory()
	{
		return this.history;
	}

	/**
	 * Return information about the one player
	 *
	 * @param player the player
	 * @return the information.
	 */
	IPlayerInfo getPlayerInfo(final AbstractPlayer player)
	{
		final PlayerInfo info = this.players.get(player);
		if (info == null)
		{
			throw new Error("No such player: " + player);
		}
		return info;
	}

	/**
	 * Refill the rack of a player.
	 *
	 * @param player Player to refill the rack
	 * @return the new drawn stones.
	 */
	private Set<Tile> refillRack(final AbstractPlayer player)
	{
		final Rack rack = this.players.get(player).rack;
		final HashSet<Tile> drawn = new HashSet<>();
		while (!this.bag.isEmpty() && rack.size() < RACK_SIZE)
		{
			final Tile tile = this.bag.poll();
			this.bag.remove(tile);
			drawn.add(tile);
			rack.add(tile);
		}
		LOGGER.trace("Remaining stones in the bag: " + this.bag.size());
		return drawn;
	}

	/**
	 * Start the game and play it until it ends.
	 */
	public void play()
	{
		if (this.players.isEmpty())
		{
			throw new IllegalStateException(MESSAGES.getString("cannot.start.game.no.player.registered"));
		}

		prepareGame();

		setState(State.STARTED);
		try
		{
			while (true)
			{
				if (this.state == State.ENDED)
				{
					synchronized (this.changing)
					{
						this.changing.wait(this.delayBeforeEnds * 1000L);
						if (this.state == State.ENDED)
						{
							break;
						}
					}
				}

				final AbstractPlayer player = this.toPlay.peekFirst();
				assert  player != null;
				LOGGER.info("Let's play " + player);
				final Play play = new Play(player);
				this.plays.add(play);
				this.waitingForPlay = new CountDownLatch(1);
				dispatch(p -> p.onPlayRequired(play));
				while (!this.waitingForPlay.await(500, TimeUnit.MILLISECONDS))
				{
					if (this.state != State.STARTED)
					{
						break;
					}
				}
			}
		}
		catch (InterruptedException e)
		{
			LOGGER.error(e, e);
		}

		dispatch(GameListener::afterGameEnd);
	}

	private void prepareGame()
	{
		fillBag();

		// Sortiert (oder mischt) die Spieler, um eine Spielreihenfolge zu definieren.
		final ArrayList<AbstractPlayer> list = new ArrayList<>(this.players.keySet());
		Collections.shuffle(list, this.random);
		final HashMap<AbstractPlayer, PlayerInfo> mapCopy = new HashMap<>(this.players);
		this.players.clear();
		for (final AbstractPlayer player : list)
		{
			this.players.put(player, mapCopy.get(player));
		}
		this.toPlay.addAll(this.players.keySet());

		// Fill racks
		for (final AbstractPlayer player : this.toPlay)
		{
			refillRack(player);
		}

		dispatch(GameListener::beforeGameStart);
	}


	/**
	 * Füllt das Säckchen mit den Buchstaben.
	 */
	private void fillBag()
	{
		// Fill bag
		final Dictionary dictionary = this.getDictionary();
		for (final Dictionary.Letter letter : dictionary.getLetters())
		{
			for (int i = 0; i < letter.prevalence; i++)
			{
				this.bag.add(dictionary.generateStone(letter.c));
			}
		}
		for (int i = 0; i < dictionary.getNumberBlanks(); i++)
		{
			this.bag.add(dictionary.generateStone(null));
		}
		Collections.shuffle(this.bag, this.random);
	}

	/**
	 * Send an event to each listener, and don't wait after an answer.
	 */
	private void dispatch(final ScrabbleEvent scrabbleEvent)
	{
		for (final GameListener player : this.listeners)
		{
			player.getIncomingEventQueue().add(scrabbleEvent);
		}
	}

	@Override
	public Dictionary getDictionary()
	{
		return this.dictionary;
	}

	@Override
	public synchronized Grid getGrid()
	{
		return this.grid;
	}


	@Override
	public synchronized Rack getRack(final AbstractPlayer player, final UUID clientKey) throws ScrabbleException
	{
		checkKey(player, clientKey);
		if (player.isObserver())
		{
			throw new ScrabbleException.InvalidStateException(MessageFormat.format(MESSAGES.getString("player.0.is.observer"), player.getName()));
		}
		return this.players.get(player).rack.copy();
	}

	@Override
	public synchronized int getScore(final AbstractPlayer player)
	{
		return this.players.get(player).getScore();
	}

	@Override
	public void editParameters(final UUID caller, final IPlayerInfo player)
	{
		if (player instanceof PlayerInfo)
		{
			((PlayerInfo) player).player.editParameters();
		}
		else
		{
			throw new IllegalArgumentException("Cannot find the player matching this info: " + player);
		}
	}

	@Override
	public void sendMessage(final AbstractPlayer sender, final String message)
	{
		dispatchMessage(MessageFormat.format(MESSAGES.getString("message.of.0.1"), sender.getName(), message));
	}

	@Override
	public void quit(final AbstractPlayer player, final UUID key, final String message) throws ScrabbleException
	{
		checkKey(player, key);
		final String msg = MessageFormat.format(MESSAGES.getString("player.0.quits.with.message.1"), player, message);
		LOGGER.info(msg);
		dispatchMessage(msg);
		setState(State.ENDED);
	}

	/**
	 * Quit the game. All listeners are informed through {@code GameListener#afterGameEnd}.
	 */
	void quitGame()
	{
		dispatch(player -> player.afterGameEnd());
	}

	@Override
	public oscrabble.configuration.Configuration getConfiguration()
	{
		return this.configuration;
	}

	/**
	 * Save a configuration.
	 *
	 * @param propertyFile output file
	 * @param gameConfig configuration of the game itself
	 * @param players list of the players to extract their configuration.
	 */
	private static void saveConfiguration(final File propertyFile, final Configuration gameConfig, final Collection<PlayerInfo> players)
	{
		try (final PrintWriter writer = new PrintWriter(new FileWriter(propertyFile)))
		{
			final Properties properties = gameConfig.asProperties();
			for (final PlayerInfo pi : players)
			{
				final oscrabble.configuration.Configuration pc = pi.player.getConfiguration();
				final Properties sp = pc == null ? new Properties() : pc.asProperties();
				sp.setProperty(KEY_METHOD, pi.player.getType().name());
				PropertyUtils.addAsSubProperties(
						properties,
						sp,
						"player." + pi.getName()
				);
			}
			properties.store(writer, "Scrabble properties");
		}
		catch (IOException e)
		{
			throw new IOError(e);
		}
	}

	@Override
	public boolean isLastPlayError(final AbstractPlayer player)
	{
		final PlayerInfo mi = this.players.get(player);
		if (mi.isLastPlayError == null)
		{
			throw new IllegalStateException("Player never has played");
		}
		return mi.isLastPlayError;
	}

	private void checkKey(final AbstractPlayer player, final UUID clientKey) throws ScrabbleException.InvalidSecretException
	{
		if (clientKey == null || !clientKey.equals(this.players.get(player).key))
		{
			throw new ScrabbleException.InvalidSecretException();
		}
	}

	State getState()
	{
		return this.state;
	}

	/**
	 * For test purposes: wait until the game has reached the end of a defined play.

	 * @param roundNr the play number to wait after the end of.
	 * @param timeout the maximum time to wait
	 * @param unit the time unit of the {@code timeout} argument
	 * @throws TimeoutException  if the waiting time elapsed before the move has ended
	 * @throws InterruptedException if the current thread is interrupted
	 *         while waiting
	 */
	void awaitEndOfPlay(final int roundNr, long timeout, TimeUnit unit) throws InterruptedException, TimeoutException
	{
		long maxTime = unit.toMillis(timeout) + System.currentTimeMillis();
		while (getRoundNr() < roundNr || !this.plays.get(roundNr - 1).isDone())
		{
			Thread.sleep(100);
			if (System.currentTimeMillis() > maxTime)
			{
				throw new TimeoutException(MessageFormat.format(MESSAGES.getString("end.of.play.0.still.not.reached.after.1.2"), roundNr, timeout, unit));
			}
		}
	}

	public int getRoundNr()
	{
		return this.plays.size();
	}

	static class PlayerInfo implements IPlayerInfo
	{
		AbstractPlayer player;

		/**
		 * Password für die Kommunikation Player &lt;&gt; Server
		 */
		UUID key;

		/**
		 * Queue  to receive events from client
		 */
		BlockingQueue<ScrabbleEvent> incomingEventQueue;

		Rack rack;
		int score;
		@Override
		public String getName()
		{
			return this.player.getName();
		}

		@Override
		public int getScore()
		{
			return this.score;
		}

		@Override
		public boolean hasEditableParameters()
		{
			return this.player.hasEditableParameters();
		}

		/**
		 * Was last play an error?
		 */
		public Boolean isLastPlayError;

		/**
		 * Last played action.
		 */
		Action lastAction;
	}

	public interface ScrabbleEvent extends Consumer<GameListener>
	{
		void accept(final GameListener player);
	}

	/**
	 * A listener
	 */
	public interface GameListener
	{
		/**
		 * Sent to all players to indicate who now has to play.
		 * @param play the play the concerned player has to play
		 */
		default void onPlayRequired(final Play play) { }

		default void onDispatchMessage(String msg) { }

		default void afterRollback() { }

		/**
		 *
		 * @param played ended play
		 */
		default void afterPlay(final Play played) { }

		default void beforeGameStart() { }

		default void afterGameEnd() { }

		Queue<ScrabbleEvent> getIncomingEventQueue();

		/**
		 * Called after the state of the game have changed
		 */
		default void onGameStateChanged() { }

		/**
		 * Called after a player has (definitively) play an non admissible play
		 * @param player player having played the non admissible play
		 * @param action the action which lead to the problem
		 */
		default void afterRejectedAction(final AbstractPlayer player, final Action action){}

	}

	/**
	 * Configuration parameters of a game. The list of players is not part of the configuration.
	 */
	private static class Configuration extends oscrabble.configuration.Configuration
	{
		@Parameter(label = "Dictionary", description = "#dictionary.of.the.game")
		Dictionary.Language dictionary = Dictionary.Language.FRENCH;

		/**
		 * Accept a new attempt after a player has tried a forbidden move
		 */
		@Parameter(label = "Retry allowed", description = "#allow.retry.on.error")
		private boolean retryAccepted;
	}

	/**
	 * Description of a played move.
	 */
	public static class HistoryEntry
	{
		private Play play;
		private final boolean errorOccurred;
		/** Points gained by this play for each player */
		private final HashMap<AbstractPlayer, Integer> scores = new HashMap<>();
		private final Set<Tile> drawn;  // to be used for rewind

		/**
		 * Information about the move at time of the action.
		 */
		private final Grid.MoveMetaInformation metaInformation;

		private HistoryEntry(final Play play, final boolean errorOccurred, final int score, final Set<Tile> drawn, final Grid.MoveMetaInformation metaInformation)
		{
			this.play = play;
			this.errorOccurred = errorOccurred;
			this.scores.put(play.player, score);
			this.drawn = drawn;
			this.metaInformation = metaInformation;
		}

		public String formatAsString()
		{
			//noinspection StringBufferReplaceableByString
			final StringBuilder sb = new StringBuilder(getPlayer().getName());
			sb.append(" - ").append(this.errorOccurred ? "*" : "").append(this.play.action.getNotation());
			sb.append(" ").append(this.scores.get(getPlayer())).append(" pts");
			return sb.toString();
		}

		public final AbstractPlayer getPlayer()
		{
			return this.play.player;
		}

		/**
		 * @return ob der Spielzug ein neu gelegtes Wort war
		 */
		public final boolean isPlayTileAction()
		{
			return this.play.action instanceof PlayTiles;
		}

		/**
		 * @return die Buchstaben des letzten Spielzugs
		 * @throws Error wenn der Spielzug nicht passt.
		 */
		public final PlayTiles getPlayTiles()
		{
			return ((PlayTiles) this.play.action);
		}
	}


	/**
	 * Problem occurring while reading / writing the configuration.
	 */
	private static final class ConfigurationException extends ScrabbleException
	{
		/**
		 * Constructor
		 * @param message message to display.
		 * @param cause the cause, if any
		 */
		private ConfigurationException(final String message, final Throwable cause)
		{
			super(message, cause);
		}

		private ConfigurationException(final String message)
		{
			this(message, null);
		}
	}


	/** Player types */
	public enum PlayerType
	{
		SWING,
		BRUTE_FORCE
	}

}
