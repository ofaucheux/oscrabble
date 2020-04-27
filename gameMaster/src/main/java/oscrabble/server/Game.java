package oscrabble.server;

import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.HashBag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.ScrabbleException;
import oscrabble.configuration.Parameter;
import oscrabble.configuration.PropertyUtils;
import oscrabble.controller.MicroServiceDictionary;
import oscrabble.data.GameState;
import oscrabble.data.IDictionary;
import oscrabble.data.objects.Grid;
import oscrabble.server.action.Action;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Game
{
	/**
	 * Resource Bundle
	 */
	public final static ResourceBundle MESSAGES = ResourceBundle.getBundle("Messages", new Locale("fr_FR"));
	public final static int RACK_SIZE = 7;
	public static final File DEFAULT_PROPERTIES_FILE = new File(System.getProperty("user.home"), ".scrabble.properties");
	private final static Logger LOGGER = LoggerFactory.getLogger(Game.class);
	private static final String SCRABBLE_MESSAGE = "Scrabble!";
	private static final String PLAYER_PREFIX = "player.";
	private static final String KEY_METHOD = "method";
	/**
	 * Seed initially used to create the random generator.
	 */
	final List<GameListener> listeners = new ArrayList<>();

	/**
	 * Players linked to their ids.
	 */
	final LinkedHashMap<String, Player> players = new LinkedHashMap<>();

	/**
	 * Plays
	 */
	final LinkedList<oscrabble.server.action.Action> actions = new LinkedList<>();

	/**
	 * Synchronize: to by synchronized by calls which change the state of the game
	 */
	final Object changing = new Object();
	private final ScoreCalculator scoreCalculator;
	/**
	 * List of the users, the first to play at head
	 */
	LinkedList<Player> toPlay = new LinkedList<>();

	private final Grid grid;
	private final Random random;
	private final LinkedList<Character> bag = new LinkedList<>();
	private final IDictionary dictionary;

	/**
	 * Configuration of the game
	 */
	private final Configuration configuration;
	/**
	 * History of the game, a line played move (even if it was an error).
	 */
	private final List<HistoryEntry> history = Collections.synchronizedList(new LinkedList<>());
	/**
	 * File to save and read the game configuration
	 */
	private final File propertyFile;
	/**
	 * Delay (in seconds) before changing the state from ENDING to ENDED
	 */
	int delayBeforeEnds = 3;
	private CountDownLatch waitingForPlay;

	/**
	 * State of the game
	 */
	private GameState.State state;

	/**
	 * Should the player play in the order they came in game or not.
	 */
	boolean randomPlayerOrder = true;


	private String assertFirstLetters;

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
			LOGGER.error("Error", configurationException);
//			TODO throw configurationException;
		}

		this.configuration = new Configuration();
		this.configuration.loadProperties(properties);
		try
		{
			this.dictionary = new MicroServiceDictionary(
					URI.create("http://localhost:8080/"),
					this.configuration.language
			);
			this.scoreCalculator = new ScoreCalculator(this.dictionary.getScrabbleRules());
		}
		catch (IllegalArgumentException e)
		{
			throw new ConfigurationException("Not known language: " + this.configuration.language);
		}
//		TODO
//		this.configuration.addChangeListener(evt -> saveConfiguration());

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
			final String methodName = playerProps.getProperty(KEY_METHOD);
//			final BruteForceMethod.Player bfPlayer;
//			switch (PlayerType.valueOf(methodName.toUpperCase()))
//			{
//				// TODO: sowewhere else
//				case BRUTE_FORCE:
//					bfPlayer = new BruteForceMethod(this.dictionary).new Player(name);
//					bfPlayer.loadConfiguration(playerProps);
//					this.addPlayer(bfPlayer);
//					break;
//				default:
//					throw new ConfigurationException("Unknown method: " + methodName);
//			}

			// TODO?
//			final oscrabble.configuration.Configuration playerConfig = player.getConfiguration();
//			if (playerConfig != null)
//			{
//				playerConfig.addChangeListener(evt -> saveConfiguration());
//			}
		}

		this.grid = new Grid(this.dictionary.getScrabbleRules());
		this.random = new Random();
		this.waitingForPlay = new CountDownLatch(1);

		setState(GameState.State.BEFORE_START);
		LOGGER.info("Created game with random seed " + this.random);
	}

	/**
	 * Constructor for test purposes a game without player.
	 *
	 * @param dictionary dictionary
	 * @param randomSeed seed to initialize the random generator
	 */
	public Game(final IDictionary dictionary, final long randomSeed)
	{
		// TODO: random weg, prüfen ob der Constructor sinnvoll ist - ist für Tests
		this.random = new Random(randomSeed);
		this.dictionary = dictionary;
//		this.sli = new ScrabbleLanguageInformation(language);
		this.grid = new Grid(dictionary.getScrabbleRules());
		this.propertyFile = null;
		this.configuration = new Configuration();
		this.state = GameState.State.BEFORE_START;
		this.scoreCalculator = new ScoreCalculator(this.dictionary.getScrabbleRules());
	}

	/**
	 * Constructor for test purposes.
	 *
	 * @param dictionary dictionary
	 * @see #Game(IDictionary, long)
	 */
	public Game(final IDictionary dictionary)
	{
		this(dictionary, new Random().nextLong());
	}

//	/**
//	 * Save the configuration of this game
//	 */
//	private void saveConfiguration()
//	{
//		saveConfiguration(this.propertyFile, this.configuration, this.players.values());
//	}

//	todo
//	/**
//	 * Save a configuration.
//	 *
//	 * @param propertyFile output file
//	 * @param gameConfig   configuration of the game itself
//	 * @param players      list of the players to extract their configuration.
//	 */
//	private static void saveConfiguration(final File propertyFile, final Configuration gameConfig, final Collection<Player> players)
//	{
//		try (final PrintWriter writer = new PrintWriter(new FileWriter(propertyFile)))
//		{
//			final Properties properties = gameConfig.asProperties();
//			for (final player pi : players)
//			{
//				final oscrabble.configuration.Configuration pc = pi.player.getConfiguration();
//				final Properties sp = pc == null ? new Properties() : pc.asProperties();
//				sp.setProperty(KEY_METHOD, pi.player.getType().name());
//				PropertyUtils.addAsSubProperties(
//						properties,
//						sp,
//						"player." + pi.getName()
//				);
//			}
//			properties.store(writer, "Scrabble properties");
//		}
//		catch (IOException e)
//		{
//			throw new IOError(e);
//		}
//	}

	/**
	 * Add a listener.
	 * @param listener listener to add
	 */
	public void addListener(final GameListener listener)
	{
		this.listeners.add(listener);
	}

	/**
	 * Add player
	 * @param jsonPlayer player
	 * @return the player
	 */
	public synchronized Player addPlayer(final oscrabble.data.Player jsonPlayer) throws ScrabbleException
	{
		final Player player = new Player();
		player.id = jsonPlayer.id;
		player.name = jsonPlayer.name;
		return addPlayer(player);
	}

	/**
	 * Add a player
	 *
	 * @param player
	 * @return the player
	 */
	<A extends Player> A addPlayer(final A player) throws ScrabbleException
	{
		if (this.players.put(player.id, player) != null)
		{
			throw new ScrabbleException("Player ID already registred");
		}
		return player;
	}

	/**
	 * Play an action.
	 * @param jsonAction action to play.
	 * @return the score
	 * @throws oscrabble.ScrabbleException
	 */
	public synchronized void play(/*final UUID clientKey, */ final oscrabble.data.Action jsonAction) throws oscrabble.ScrabbleException
	{
		synchronized (this.changing)
		{

			// TODO
//			checkKey(jsonAction.player, clientKey);

			final Action action = Action.parse(jsonAction);
			final Player player = this.players.get(jsonAction.player);
			play(player, action);
		}
	}

	/**
	 * Play an action
	 *
	 * @param action
	 * @param player
	 * @return score
	 * @throws ScrabbleException.ForbiddenPlayException
	 * @throws ScrabbleException.NotInTurn
	 */
	void play(final Player player, final Action action) throws ScrabbleException.ForbiddenPlayException, ScrabbleException.NotInTurn
	{
		if (player == null)
		{
			throw new ScrabbleException.ForbiddenPlayException("Unknown player: " + player.name);
		}

		if (this.toPlay.peekFirst() != player)
		{
			throw new ScrabbleException.NotInTurn(player.name);
		}

		LOGGER.info(player.name + " plays " + action.notation);

		int score = 0;  // TODO: should not be there. History should not use score anymore
		boolean actionRejected = false;
		ScoreCalculator.MoveMetaInformation moveMI = null;
		boolean done = false;
		final Set<Character> drawn;
		try
		{
			final ArrayList<String> messages = new ArrayList<>(5);

			if (action instanceof Action.PlayTiles)
			{
				final Action.PlayTiles playTiles = (Action.PlayTiles) action;

				// check possibility
				moveMI = this.scoreCalculator.getMetaInformation(this.grid, playTiles);
				final HashBag<Character> remaining = new HashBag<>(player.rack);

				final List<Character> requiredLetters = moveMI.requiredLetter;
				for (final Character c : requiredLetters)
				{
					if (!remaining.remove(c, 1))
					{
						if (!remaining.remove(' ', 1))
						{
							throw new ScrabbleException.ForbiddenPlayException
									(MessageFormat.format(MESSAGES.getString("html.rack.with.0.br.has.not.the.required.stones.1"), player.rack, requiredLetters));
						}
					}
				}

				// first word in the middle
				if (this.grid.isEmpty() && !containsCentralField(playTiles))
				{
					throw new ScrabbleException.ForbiddenPlayException(MESSAGES.getString("the.first.word.must.be.on.the.center.square"));
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
					if (!this.dictionary.isAdmissible(crossword.toUpperCase()))
					{
						final String details = MessageFormat.format(MESSAGES.getString("word.0.is.not.allowed"), crossword);
						throw new ScrabbleException.ForbiddenPlayException(details);
					}
				}

				Grid.Square sq = this.grid.get(playTiles.startSquare);
				for (int i = 0; i < playTiles.word.length(); i++)
				{
					final char c = playTiles.word.charAt(i);
					if (sq.isEmpty())
					{
						sq.c = c;
					}
					else
					{
						assert Character.toUpperCase(sq.c) == Character.toUpperCase(c); //  sollte schon oben getestet sein.
					}

					sq = sq.getNeighbour(playTiles.startSquare.direction, 1);
				}

				player.rack.clear();
				player.rack.addAll(remaining);

				score = moveMI.getScore();
				if (moveMI.isScrabble)
				{
					messages.add(SCRABBLE_MESSAGE);
				}
				LOGGER.info(MessageFormat.format(MESSAGES.getString("0.plays.1.for.2.points"), player.name, playTiles.notation, score));
			}
			else if (action instanceof Action.Exchange)
			{
				if (this.bag.size() < this.dictionary.getScrabbleRules().requiredTilesInBagForExchange)
				{
					throw new ScrabbleException.ForbiddenPlayException(MESSAGES.getString("not.enough.tiles.in.bag.for.exchange"));
				}

				final Action.Exchange exchange = (Action.Exchange) action;
				final HashBag<Character> newRack = new HashBag<>(player.rack);
				for (final char ex : exchange.toExchange)
				{
					if (!newRack.remove(ex))
					{
						throw new ScrabbleException.ForbiddenPlayException("No (or not enough) character " + ex + " to exchange it");
					}
				}

				for (final char c : exchange.toExchange)
				{
					this.bag.add(c);
				}
				Collections.shuffle(this.bag, this.random);
				moveMI = null;
				LOGGER.info(player.name + " exchanges " + exchange.toExchange.length + " stones");
			}
			else if (action instanceof Action.SkipTurn)
			{
				LOGGER.info(player.name + " skips its turn");
				this.dispatchMessage(MessageFormat.format(MESSAGES.getString("0.skips.its.turn"), player.name));
			}
			else
			{
				throw new AssertionError("Command not treated: " + action);
			}

			player.isLastPlayError = false;
			messages.forEach(message -> dispatchMessage(message));

//				LOGGER.debug("Grid after play move nr #" + action.uuid + ":\n" + this.grid.asASCIIArt());
			actionRejected = false;
			player.score += score;
			done = true;
		}
		catch (final ScrabbleException e)
		{
			LOGGER.info("Refuse play: " + action + ". Cause: " + e);
			actionRejected = true;
//				player.onDispatchMessage(e.getLocalizedMessage());
			if (this.configuration.retryAccepted /* TODO: several places for blanks || e.acceptRetry()*/)
			{
				this.dispatch(p -> p.onPlayRequired(player));
				score = 0;
			}
			else
			{
//					dispatch(listener -> listener.afterRejectedAction(player, action));
				player.isLastPlayError = true;
				done = true;
			}
		}
		finally
		{
			if (done)
			{
				drawn = refillRack(player);
				player.lastAction = action;
				this.actions.add(action);
				dispatch(toInform -> toInform.afterPlay(action));
				final HistoryEntry historyEntry = new HistoryEntry(player, action, actionRejected, score, drawn, moveMI);
				this.history.add(historyEntry);
				this.toPlay.pop();
				this.toPlay.add(player);

				if (player.rack.isEmpty())
				{
					endGame(player, historyEntry);
				}
				else if (action instanceof Action.SkipTurn)
				{
					// Quit if nobody can play
					final AtomicBoolean canPlay = new AtomicBoolean(false);
					this.players.forEach((p, mi) -> {
						if (mi.lastAction instanceof Action.SkipTurn) canPlay.set(true);
					});
					if (!canPlay.get())
					{
						endGame(null, historyEntry);
					}
				}
				this.waitingForPlay.countDown();
			}
		}
	}

	public synchronized void rollbackLastMove(final Player caller) throws ScrabbleException
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

			final Player rollbackedPlayer = historyEntry.player;
			rollbackedPlayer.rack.removeAll(historyEntry.drawn);
			historyEntry.metaInformation.getFilledSquares().forEach(
					square -> {
						rollbackedPlayer.rack.add(square.c);
						square.c = null;
					}
			);

			this.players.values().forEach(p ->
					p.score -= historyEntry.scores.getOrDefault(p, 0)
			);
			assert this.toPlay.peekLast() == rollbackedPlayer;

			if (this.state == GameState.State.STARTED)
			{
				this.actions.removeLast();
			}

			this.toPlay.removeLast();
			this.toPlay.addFirst(rollbackedPlayer);
//			dispatch(toInform -> toInform.afterRollback());

			setState(GameState.State.STARTED);

			this.waitingForPlay.countDown();
			this.changing.notify();
		}

	}

//	//	public void playerConfigHasChanged(final Player player, final UUID playerKey)
//	{
//		saveConfiguration();
//	}

	public synchronized Player getPlayerToPlay()
	{
		return this.toPlay.getFirst();
	}

	/**
	 * Ends the game.
	 *
	 * @param firstEndingPlayer player which has first emptied its rack, or {@code null} if nobody has cleared it.
	 */
	private synchronized void endGame(final Player firstEndingPlayer, final HistoryEntry historyEntry)
	{
		LOGGER.info("Games ends. Player which have clear its rack: " + (firstEndingPlayer == null ? null : firstEndingPlayer.name));
		final StringBuffer message = new StringBuffer();
		if (firstEndingPlayer == null)
		{
			message.append(MESSAGES.getString("game.ends.without.any.player.have.cleared.its.rack"));
		}
		else
		{
			message.append(MessageFormat.format(MESSAGES.getString("0.has.cleared.its.rack"), firstEndingPlayer.name)).append('\n');
		}
		this.players.forEach(
				(dummy, player) ->
				{
					if (player != firstEndingPlayer)
					{
						int gift = 0;
						for (final Character tile : player.rack)
						{
							gift += this.dictionary.getScrabbleRules().letters.get(tile).points;
						}
						player.score -= gift;
						historyEntry.scores.put(player, -gift);
						if (firstEndingPlayer != null)
						{
							firstEndingPlayer.score += gift;
							historyEntry.scores.put(firstEndingPlayer, historyEntry.scores.get(firstEndingPlayer) + gift);
						}
						message.append(MessageFormat.format(MESSAGES.getString("0.gives.1.points"), player.name, gift)).append("\n");
					}
				});

//		dispatch(c -> c.onDispatchMessage(message.toString()));
		setState(GameState.State.ENDED);
	}


	/**
	 * Send a message to each listener.
	 *
	 * @param message message to dispatch
	 */
	private void dispatchMessage(final String message)
	{
//		dispatch(l -> l.onDispatchMessage(message));
	}

	public List<Player> getPlayers()
	{
		return new ArrayList<>(this.players.values());
	}

	public Iterable<HistoryEntry> getHistory()
	{
		return this.history;
	}

	/**
	 * Refill the rack of a player.
	 *
	 * @param player Player to refill the rack
	 * @return list of drawn tiles.
	 */
	private Set<Character> refillRack(final Player player)
	{
		final Set<Character> drawn = new HashSet<>();
		while (!this.bag.isEmpty() && player.rack.size() < RACK_SIZE)
		{
			final Character poll = this.bag.poll();
			drawn.add(poll);
			player.rack.add(poll);
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

		setState(GameState.State.STARTED);
		try
		{
			while (true)
			{
				if (this.state == GameState.State.ENDED)
				{
					synchronized (this.changing)
					{
						this.changing.wait(this.delayBeforeEnds * 1000L);
						if (this.state == GameState.State.ENDED)
						{
							break;
						}
					}
				}

				final Player player = this.toPlay.peekFirst();
				assert player != null;
				LOGGER.info("Let's play " + player);
				this.waitingForPlay = new CountDownLatch(1);
				dispatch(p -> p.onPlayRequired(player));
				while (!this.waitingForPlay.await(500, TimeUnit.MILLISECONDS))
				{
					if (this.state != GameState.State.STARTED)
					{
						break;
					}
				}
			}
		}
		catch (InterruptedException e)
		{
			LOGGER.error(e.toString(), e);
		}

//		dispatch(GameListener::afterGameEnd);
	}

	private void prepareGame()
	{
		fillBag();

		// Sortiert (oder mischt) die Spieler, um eine Spielreihenfolge zu definieren.
//		final ArrayList<Player> list = new ArrayList<>(this.players.values());
//		Collections.shuffle(list, this.random);
//		final HashSet<Player> mapCopy = new HashSet<>(this.players);
//		this.players.clear();
//		for (final Player player : list)
//		{
//			this.players.put(player.id, player);
//		}
		this.toPlay.addAll(this.players.values());
		if (this.randomPlayerOrder)
		{
			Collections.shuffle(this.toPlay);
		}

		// Fill racks
		for (final Player player : this.toPlay)
		{
			refillRack(player);
		}

//		dispatch(GameListener::beforeGameStart);
	}


	/**
	 * Füllt das Säckchen mit den Buchstaben.
	 */
	private void fillBag()
	{
		// Fill bag
		this.dictionary.getScrabbleRules().letters.forEach(
				(letter, info) ->
				{
					for (int i = 0; i < info.prevalence; i++)
					{
						this.bag.add(letter);
					}
				}
		);
		for (int i = 0; i < this.dictionary.getScrabbleRules().numberBlanks; i++)
		{
			this.bag.add(' ');
		}
		Collections.shuffle(this.bag, this.random);

		if ((this.assertFirstLetters != null))
		{

			final ArrayList<Character> start = new ArrayList<>();
			final ArrayList<Character> remains = new ArrayList<>(this.bag);

			for (final char c : this.assertFirstLetters.toCharArray())
			{
				if (!remains.remove((Character) c))
				{
					throw new IllegalStateException("Not enough letter " + c + " remaining in the bag");
				}
				start.add(c);
			}

			this.bag.clear();
			this.bag.addAll(start);
			this.bag.addAll(remains);
		}


	}

	/**
	 * Send an event to each listener, and don't wait after an answer.
	 */
	private void dispatch(final ScrabbleEvent scrabbleEvent)
	{
		for (final GameListener listener : this.listeners)
		{
			final Queue<ScrabbleEvent> queue = listener.getIncomingEventQueue();
			if (queue != null)
			{
				// in the queue
				queue.add(scrabbleEvent);
			}
			else
			{
				// on the same thread
				scrabbleEvent.accept(listener);
			}
		}
	}

	public IDictionary getDictionary()
	{
		return this.dictionary;
	}

	public synchronized Grid getGrid()
	{
		return this.grid;
	}


	public synchronized int getScore(final Player player)
	{
		return player.score;
	}

// TOdo?
//	public void editParameters(final UUID caller, final Iplayer player)
//	{
//		if (player instanceof player)
//		{
//			((player) player).player.editParameters();
//		}
//		else
//		{
//			throw new IllegalArgumentException("Cannot find the player matching this info: " + player);
//		}
//	}


	public void quit(final Player player, final String secret, final String message) throws ScrabbleException
	{
		checkSecret(player, secret);
		final String msg = MessageFormat.format(MESSAGES.getString("player.0.quits.with.message.1"), player, message);
		LOGGER.info(msg);
		dispatchMessage(msg);
		setState(GameState.State.ENDED);
	}

	/**
	 * Quit the game. All listeners are informed through {@code GameListener#afterGameEnd}.
	 */
	void quitGame()
	{
//		dispatch(player -> player.afterGameEnd());
	}

	public oscrabble.configuration.Configuration getConfiguration()
	{
		return this.configuration;
	}

	private void checkSecret(final Player player, final String secret) throws ScrabbleException.InvalidSecretException
	{
		// TODO
//		if (secret == null || !secret.equals(player.secret))
//		{
//			throw new ScrabbleException.InvalidSecretException();
//		}
	}

	GameState.State getState()
	{
		return this.state;
	}

	public void setState(final GameState.State state)
	{
		if (this.state != state)
		{
			this.state = state;
//			this.listeners.forEach(l -> l.onGameStateChanged());
		}
	}

	/**
	 * For test purposes: wait until the game has reached the end of a defined play.
	 *
	 * @param roundNr the play number to wait after the end of.
	 * @param timeout the maximum time to wait
	 * @param unit    the time unit of the {@code timeout} argument
	 * @throws TimeoutException     if the waiting time elapsed before the move has ended
	 * @throws InterruptedException if the current thread is interrupted while waiting
	 */
	void awaitEndOfPlay(final int roundNr, long timeout, TimeUnit unit) throws InterruptedException, TimeoutException
	{
		long maxTime = unit.toMillis(timeout) + System.currentTimeMillis();
		while (getRoundNr() < roundNr /* || todo !this.actions.get(roundNr - 1) */)
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
		return this.actions.size();
	}

	private boolean containsCentralField(final Action.PlayTiles move) throws ScrabbleException.ForbiddenPlayException
	{
		// todo: beiing done.
		final int center = (int) Math.ceil(this.dictionary.getScrabbleRules().gridSize / 2f);
		final int length = move.word.length();

		Grid.Coordinate c = move.startSquare;
		switch (c.direction)
		{
			case VERTICAL:
				return (c.x == center && (c.y <= center && (c.y + length - 1) >= center));
			case HORIZONTAL:
				return (c.y == center && (c.x <= center && (c.y + length - 1) >= center));
			default:
				throw new AssertionError();
		}
	}

	/**
	 * Sort (the beginning) of the bag to asset the next letters
	 * @param letters the next letters
	 */
	public void assertFirstLetters(final String letters)
	{
		assert this.state == GameState.State.BEFORE_START;
		this.assertFirstLetters = letters;
	}

	/**
	 * cf. {@link Game#awaitEndOfPlay(int, long, TimeUnit)}
	 */
	public void awaitEndOfPlay(final int roundNr) throws TimeoutException, InterruptedException
	{
		awaitEndOfPlay(roundNr, 30, TimeUnit.SECONDS);
	}

	/**
	 * Player types
	 */
	public enum PlayerType
	{
		SWING,
		BRUTE_FORCE
	}

//	public interface ScrabbleEvent extends Consumer<GameListener>
//	{
//		void accept(final GameListener player);
//	}

//	/**
//	 * A listener
//	 */
//	public interface GameListener
//	{
//		/**
//		 * Sent to all players to indicate who now has to play.
//		 *
//		 * @param play the play the concerned player has to play
//		 */
//		default void onPlayRequired(final Play play)
//		{
//		}
//
//		default void onDispatchMessage(String msg)
//		{
//		}
//
//		default void afterRollback()
//		{
//		}
//
//		/**
//		 * @param played ended play
//		 */
////		default void afterPlay(final Play played) { }
////
////		default void beforeGameStart() { }
////
////		default void afterGameEnd() { }
//
//		Queue<ScrabbleEvent> getIncomingEventQueue();
//
//		/**
//		 * Called after the state of the game have changed
//		 */
//		default void onGameStateChanged()
//		{
//		}
//
//		/**
//		 * Called after a player has (definitively) play an non admissible play
//		 *
//		 * @param player player having played the non admissible play
//		 * @param action the action which lead to the problem
//		 */
//		default void afterRejectedAction(final Player player, final Action action)
//		{
//		}
//
//	}

	public static class Player
	{

		/**
		 * Was last play an error?
		 */
		public boolean isLastPlayError;

		/**
		 * Name
		 */
		String name;

		/**
		 * Password für die Kommunikation Player &lt;&gt; Server
		 */
		String id;

//		/**
//		 * Queue  to receive events from client
//		 */
//		BlockingQueue<ScrabbleEvent> incomingEventQueue;

		/**
		 * Tiles in the rack, space for a joker.
		 */
		final Bag<Character> rack = new HashBag<>();

		int score;
		/**
		 * Last played action.
		 */
		oscrabble.server.action.Action lastAction;

		// TODO
		public oscrabble.configuration.Configuration getConfiguration()
		{
			return null;
		}

		@Override
		public String toString()
		{
			return "Player{" +
					"name='" + this.name + '\'' +
					'}';
		}
	}

	/**
	 * Configuration parameters of a game. The list of players is not part of the configuration.
	 */
	private static class Configuration extends oscrabble.configuration.Configuration
	{
		@Parameter(label = "Dictionary", description = "#dictionary.of.the.game")
		String language = "FRENCH";

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
		private final boolean errorOccurred;

		/**
		 * Points gained by this play for each player
		 */
		private final HashMap<Player, Integer> scores = new HashMap<>();

		private final Set<Character> drawn;  // to be used for rewind
		/**
		 * Information about the move at time of the action.
		 */
		private final ScoreCalculator.MoveMetaInformation metaInformation;
		private final Player player;
		private Action play;

		private HistoryEntry(final Player player, final Action play, final boolean errorOccurred, final int score, final Set<Character> drawn, final ScoreCalculator.MoveMetaInformation metaInformation)
		{
			this.player = player;
			this.play = play;
			this.errorOccurred = errorOccurred;
			this.scores.put(player, score);
			this.drawn = drawn;
			this.metaInformation = metaInformation;
		}

		public String formatAsString()
		{
			//noinspection StringBufferReplaceableByString
			final StringBuilder sb = new StringBuilder(this.player.name);
			sb.append(" - ").append(this.errorOccurred ? "*" : "").append(this.play.notation);
			sb.append(" ").append(this.scores.get(this.player)).append(" pts");
			return sb.toString();
		}

		/**
		 * @return ob der Spielzug ein neu gelegtes Wort war
		 */
		public final boolean isPlayTileAction()
		{
			return this.play instanceof Action.PlayTiles;
		}

//		/**
//		 * @return die Buchstaben des letzten Spielzugs
//		 * @throws Error wenn der Spielzug nicht passt.
//		 */
//		public final PlayTiles getPlayTiles()
//		{
//			return ((PlayTiles) this.play.action);
//		}
	}

	/**
	 * Problem occurring while reading / writing the configuration.
	 */
	private static final class ConfigurationException extends oscrabble.ScrabbleException
	{
		/**
		 * Constructor
		 *
		 * @param message message to display.
		 * @param cause   the cause, if any
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

}
