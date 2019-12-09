package oscrabble.server;

import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.HashBag;
import org.apache.commons.collections4.bag.TreeBag;
import org.apache.log4j.Logger;
import oscrabble.*;
import oscrabble.client.Exchange;
import oscrabble.configuration.Parameter;
import oscrabble.dictionary.Dictionary;
import oscrabble.player.AbstractPlayer;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class Game implements IGame
{
	private final static Logger LOGGER = Logger.getLogger(Game.class);

	public final static int RACK_SIZE = 7;
	private static final String SCRABBLE_MESSAGE = "Scrabble!";
	public static final ScheduledExecutorService SERVICE = Executors.newScheduledThreadPool(3);

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
	 * Current play
	 */
	Play currentPlay;

	/**
	 * History of the game, a line played move (even if it was an error).
	 */
	private final LinkedList<HistoryEntry> history = new LinkedList<>();

	/**
	 * Schedule task to change the state from ENDING to ENDED
	 */
	private ScheduledFuture<Void> endScheduler;

	/**
	 * Synchronize: to by synchronized by calls which change the state of the game
	 */
	final Object changing = new Object();

	/**
	 * Secret to be transmitted to the currently playing player.
	 */
	private String currentSecret;

	public Game(final Dictionary dictionary, final long randomSeed)
	{
		this.configuration = new Configuration();
		this.configuration.setValue("retryAccepted", true);
		this.dictionary = dictionary;
		this.grid = new Grid(this.dictionary);
		this.randomSeed = randomSeed;
		this.random = new Random(randomSeed);
		this.waitingForPlay = new CountDownLatch(1);

		setState(State.BEFORE_START);
		LOGGER.info("Created game with random seed " + randomSeed);
	}

	private void setState(final State state)
	{
		if (this.state != state)
		{
			this.state = state;
			this.listeners.forEach( l -> l.onGameStateChanged());
		}
	}

	public Game(final Dictionary dictionary)
	{
		this(dictionary, new Random().nextLong());
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
	public synchronized int play(final UUID clientKey, final Play play, final Action action) throws ScrabbleException
	{
		synchronized (this.changing)
		{
			if (this.currentPlay != play)
			{
				throw new ScrabbleException.InvalidStateException("Not current turn");
			}

			assert !this.currentPlay.done;
			final AbstractPlayer player = this.currentPlay.player;
			final PlayerInfo playerInfo = players.get(player);
			checkKey(player, clientKey);

			LOGGER.info(player.getName() + " plays " + action);
			int score = 0;
			boolean actionRejected = false;
			Set<Tile> drawn = null;
			Grid.MoveMetaInformation moveMI = null;
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
						throw new ScrabbleException.ForbiddenPlayException("<html>Rack with " + rackLetters + "<br>has not the required stones " + requiredLetters);
					}

					// check touch
					if (this.grid.isEmpty())
					{
						if (playTiles.word.length() < 2)
						{
							throw new ScrabbleException.ForbiddenPlayException("First word must have at least two letters");
						}
					}
					else if (moveMI.crosswords.isEmpty() && requiredLetters.size() == playTiles.word.length())
					{
						throw new ScrabbleException.ForbiddenPlayException("New word must touch another one");
					}

					// check dictionary
					final Set<String> toTest = new LinkedHashSet<>();
					toTest.add(playTiles.word);
					toTest.addAll(moveMI.crosswords);
					for (final String crossword : toTest)
					{
						if (!this.dictionary.containUpperCaseWord(crossword.toUpperCase()))
						{
							final String details = "Word \"" + crossword + "\" is not allowed";
							throw new ScrabbleException.ForbiddenPlayException(details);
						}
					}

					if (this.grid.isEmpty())
					{
						final Grid.Square center = this.grid.getCenter();
						if (!playTiles.getSquares().containsKey(center))
						{
							throw new ScrabbleException.ForbiddenPlayException("The first word must be on the center square");
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
					LOGGER.info(playerInfo.getName() + " plays \"" + playTiles.getNotation() + "\" for " + score + " points");
				}
				else if (action instanceof Exchange)
				{
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
					this.dispatchMessage(playerInfo.getName() + " skips its turn");
				}
				else
				{
					throw new AssertionError("Command not treated: " + action);
				}

				playerInfo.isLastPlayError = false;
				drawn = refillRack(playerInfo.player);
				messages.forEach(message -> dispatchMessage(message));

				LOGGER.debug("Grid after play move nr #" + this.currentPlay.uuid + ":\n" + this.grid.asASCIIArt());
				actionRejected = false;
				this.currentPlay.done = true;
				return score;
			}
			catch (final ScrabbleException e)
			{
				LOGGER.info("Refuse play: " + action + ". Cause: " + e);
				actionRejected = true;
				playerInfo.player.onDispatchMessage(e.toString());
				if (this.configuration.retryAccepted /* TODO: several places for blanks || e.acceptRetry()*/)
				{
					playerInfo.player.onDispatchMessage("Retry accepted");
				}
				else
				{
					dispatch(listener -> listener.afterRejectedAction(playerInfo.player, action));
					this.currentPlay.done = true;
					playerInfo.isLastPlayError = true;
				}
				return 0;
			}
			finally
			{
				playerInfo.lastAction = action;
				final int copyScore = score;
				dispatch(toInform -> toInform.afterPlay(play));
				if (this.currentPlay.done)
				{
					final HistoryEntry historyEntry = new HistoryEntry(this.currentPlay, actionRejected, score, drawn, moveMI);
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
				}
				this.waitingForPlay.countDown();
			}
		}
	}

	public synchronized void rollbackLastMove(final AbstractPlayer caller, final UUID callerKey) throws ScrabbleException
	{
		synchronized (this.changing)
		{
			LOGGER.info("Rollback last move on demand of " + caller);
			final HistoryEntry historyEntry = this.history.pollLast();
			if (historyEntry == null)
			{
				throw new ScrabbleException.InvalidStateException( "No move played for the time");
			}

			if (this.endScheduler != null)
			{
				this.endScheduler.cancel(true);
				this.endScheduler = null;
				setState(State.STARTED);
			}

			final PlayerInfo playerInfo = this.players.get(historyEntry.getPlayer());
			playerInfo.rack.removeAll(historyEntry.drawn);
			historyEntry.metaInformation.getFilledSquares().forEach(
					filled -> playerInfo.rack.add(filled.tile)
			);
			this.grid.remove(historyEntry.metaInformation);
			this.players.forEach( (p,info) -> info.score -= historyEntry.scores.getOrDefault(p, 0));
			assert this.toPlay.peekLast() == historyEntry.getPlayer();
			this.toPlay.removeLast();
			this.toPlay.addFirst(historyEntry.getPlayer());
			this.currentPlay = new Play(historyEntry.play.roundNr, this.toPlay.getFirst());
			setState(State.STARTED);
			dispatch(toInform -> toInform.afterRollback());
			dispatch(toInform -> toInform.onPlayRequired(this.currentPlay));

			this.waitingForPlay.countDown();
		}

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
		assert this.endScheduler == null;
		setState(State.ENDING);
		final StringBuffer message = new StringBuffer();
		if (firstEndingPlayer == null)
		{
			message.append("Game ends without any player have cleared its rack");
		}
		else
		{
			message.append(firstEndingPlayer.getName()).append(" has cleared its rack.\n");
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
						message.append(info.getName()).append(" gives ").append(gift).append(" points\n");
					}
				});

		dispatch(c -> c.onDispatchMessage(message.toString()));
		this.endScheduler = SERVICE.schedule(() -> {
					setState(State.ENDED);
					return null;
				},
				delayBeforeEnds,
				TimeUnit.SECONDS);
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
		return List.copyOf(this.players.values());
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

	@Override
	public void markAsIllegal(final String word)
	{
		this.getDictionary().markAsIllegal(word);
		dispatch(GameListener::onDictionaryChange);
	}

	public void startGame() throws ScrabbleException
	{
		if (this.players.isEmpty())
		{
			throw new IllegalStateException("Cannot start game: no player registered");
		}

		prepareGame();

		setState(State.STARTED);
		try
		{
			while (this.state != State.ENDED && this.state != State.ENDING)
			{
				this.currentPlay = new Play(
						this.currentPlay == null ? 1 : this.currentPlay.roundNr + (this.currentPlay.done ? 1 : 0),
						this.toPlay.getFirst()
				);
				final AbstractPlayer player = this.toPlay.peekFirst();
				assert  player != null;
				LOGGER.info("Let's play " + player);
				this.waitingForPlay = new CountDownLatch(1);
				dispatch(p -> p.onPlayRequired(this.currentPlay));
				this.waitingForPlay.await();
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
			throw new ScrabbleException.InvalidStateException("Player " + player.getName() + " is observer");
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
		dispatchMessage("Message of " + sender.getName() + ": " + message);
	}

	@Override
	public void quit(final AbstractPlayer player, final UUID key, final String message) throws ScrabbleException
	{
		checkKey(player, key);
		final String msg = "Player " + player + " quits with message \"" + message + "\"";
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

	private void checkKey(final AbstractPlayer player, final UUID clientKey) throws ScrabbleException
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
		while (this.currentPlay.roundNr <= roundNr)
		{
			Thread.sleep(100);
			if (System.currentTimeMillis() > maxTime)
			{
				throw new TimeoutException("End of play " + roundNr + " still not reached after " + timeout + " " + unit);
			}
		}
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

	/**
	 * State of the game
	 */
	enum  State
	{BEFORE_START, STARTED, ENDING, ENDED}

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

		default void onDictionaryChange() { }

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
		 * @param player
		 * @param action
		 */
		default void afterRejectedAction(final AbstractPlayer player, final Action action){}

	}

	/**
	 * Configuration parameters.
	 */
	private static class Configuration extends oscrabble.configuration.Configuration
	{
		/**
		 * Accept a new attempt after a player has tried a forbidden move
		 */
		@Parameter(label = "Retry allowed", description = "Allow retry on error")
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
			final StringBuilder sb = new StringBuilder(getPlayer().getName());
			sb.append(" - ").append(this.errorOccurred ? "*" : "").append(((PlayTiles) this.play.action).getNotation());
			sb.append(" ").append(this.scores.get(getPlayer())).append(" pts");
			return sb.toString();
		}

		public final AbstractPlayer getPlayer()
		{
			return this.play.player;
		}
	}
}
