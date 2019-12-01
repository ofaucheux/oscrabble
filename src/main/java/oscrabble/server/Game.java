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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class Game implements IGame
{
	private final static Logger LOGGER = Logger.getLogger(Game.class);

	public final static int RACK_SIZE = 7;
	private static final String SCRABBLE_MESSAGE = "Scrabble!";

	private final Map<AbstractPlayer, PlayerInfo> players = new LinkedHashMap<>();
	private final LinkedList<AbstractPlayer> toPlay = new LinkedList<>();
	private final Grid grid;
	private final Random random;
	private CountDownLatch waitingForPlay;
	private final LinkedList<Stone> bag = new LinkedList<>();
	private final Dictionary dictionary;

	final List<GameListener> listeners = new ArrayList<>();

	/**
	 * State of the game
	 */
	private final AtomicReference<State> state = new AtomicReference<>();

	/**
	 * Configuration of the game
	 */
	private final Configuration configuration;

	/**
	 * Number of current move (Starting with 1).
	 */
	private int moveNr;

	/**
	 * History of the game, a line played move (even if it was an error).
	 */
	private final LinkedList<HistoryEntry> history = new LinkedList<>();

	Game(final Dictionary dictionary, final long randomSeed)
	{
		this.configuration = new Configuration();
		this.configuration.setValue("retryAccepted", true);
		this.dictionary = dictionary;
		this.grid = new Grid(this.dictionary);
		this.random = new Random(randomSeed);
		this.waitingForPlay = new CountDownLatch(1);

		this.state.set(State.BEFORE_START);
		LOGGER.info("Created game with random seed " + randomSeed);
	}

	public Game(final Dictionary dictionary)
	{
		this(dictionary, new Random().nextLong());
	}

	@Override
	public synchronized void register(final AbstractPlayer newPlayer)
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
	public synchronized int play(final AbstractPlayer player, final IAction action)
	{
		if (player != getPlayerToPlay())
		{
			throw new IllegalStateException("The player " + player.toString() + " is not playing");
		}

		final PlayerInfo playerInfo = this.players.get(player);
		boolean done = false;
		int score = 0;
		boolean errorOccurred = false;
		Set<Stone> drawn = null;
		Grid.MoveMetaInformation moveMI = null;
		try
		{
			final ArrayList<String> messages = new ArrayList<>(5);

			if (action instanceof Move)
			{
				final Move move = (Move) action;

				// check possibility
				moveMI = this.grid.getMetaInformation(move);
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
					final String details = "<html>Rack with " + rackLetters + "<br>has not the required stones " + requiredLetters;
					player.onDispatchMessage(details);
					throw new ScrabbleException(ScrabbleException.ERROR_CODE.MISSING_LETTER);
				}

				// check touch
				if (this.grid.isEmpty())
				{
					if (move.word.length() < 2)
					{
						throw new ScrabbleException(ScrabbleException.ERROR_CODE.FORBIDDEN, "First word must have at least two letters");
					}
				}
				else if (moveMI.crosswords.isEmpty() && requiredLetters.size() == move.word.length())
				{
					throw new ScrabbleException(ScrabbleException.ERROR_CODE.FORBIDDEN, "New word must touch another one");
				}

				// check dictionary
				final Set<String> toTest = new LinkedHashSet<>();
				toTest.add(move.word);
				toTest.addAll(moveMI.crosswords);
				for (final String crossword : toTest)
				{
					if (!this.dictionary.containUpperCaseWord(crossword.toUpperCase()))
					{
						final String details = "Word \"" + crossword + "\" is not allowed";
						throw new ScrabbleException(ScrabbleException.ERROR_CODE.FORBIDDEN, details);
					}
				}

				if (this.grid.isEmpty())
				{
					final Grid.Square center = this.grid.getCenter();
					if (!move.getSquares().containsKey(center))
					{
						throw new ScrabbleException(ScrabbleException.ERROR_CODE.FORBIDDEN,
								"The first word must be on the center square");
					}
				}

				Grid.Square square = move.startSquare;
				for (int i = 0; i < move.word.length(); i++)
				{
					final char c = move.word.charAt(i);
					if (square.isEmpty())
					{
						final Stone stone =
								playerInfo.rack.removeStones(
										Collections.singletonList(move.isPlayedByBlank(i) ? ' ' : c)
								).get(0);
						if (stone.isJoker())
						{
							stone.setCharacter(c);
						}
						this.grid.set(square, stone);
						stone.setSettingAction(action);
					}
					else
					{
						assert square.stone.getChar() == c; //  sollte schon oben getestet sein.
					}
					square = square.getFollowing(move.getDirection());
				}

				score = moveMI.getScore();
				if (moveMI.isScrabble)
				{
					messages.add(SCRABBLE_MESSAGE);
				}
				playerInfo.score += score;
				LOGGER.info(player.getName() + " plays \"" + move.getNotation() + "\" for " + score + " points");
			}
			else if (action instanceof Exchange)
			{
				final List<Stone> stones1 = playerInfo.rack.removeStones(((Exchange) action).getChars());
				this.bag.addAll(stones1);
				Collections.shuffle(this.bag, this.random);
				moveMI = null;
			}
			else
			{
				throw new AssertionError("Command not treated: " + action);
			}

			playerInfo.isLastPlayError = false;
			drawn = refillRack(player);
			messages.forEach(message -> dispatchMessage(message));

			LOGGER.debug("Grid after play move nr #" + this.moveNr + ":\n" + this.grid.asASCIIArt());
			errorOccurred = false;
			done = true;
			return score;
		}
		catch (final ScrabbleException e)
		{
			LOGGER.info("Refuse play: " + action + ". Cause: " + e);
			errorOccurred = true;
			player.onDispatchMessage(e.toString());
			if (this.configuration.retryAccepted || e.acceptRetry())
			{
				player.onDispatchMessage("Retry accepted");
				done = false;
			}
			else
			{
				dispatchMessage("Player " + player + " would have play an illegal move: " + e + ". Skip its turn");
				done = true;
				playerInfo.isLastPlayError = true;
			}
			return 0;
		}
		finally
		{
			HistoryEntry historyEntry = null;
			if (done)
			{
				historyEntry = new HistoryEntry(player, action, errorOccurred, score, drawn, moveMI);
				this.history.add(historyEntry);
				this.toPlay.pop();
				this.toPlay.add(player);
				this.moveNr++;
			}
			this.waitingForPlay.countDown();
			final int copyScore = score;
			dispatch(toInform -> toInform.afterPlay(this.moveNr, playerInfo, action, copyScore));
			if (done && playerInfo.rack.isEmpty())
			{
				endGame(playerInfo, historyEntry);
			}
		}
	}

	public synchronized void rollbackLastMove(final AbstractPlayer caller, final UUID callerKey) throws ScrabbleException
	{
		final HistoryEntry historyEntry = this.history.pollLast();
		if (historyEntry == null)
		{
			throw new ScrabbleException(ScrabbleException.ERROR_CODE.ASSERTION_FAILED, "No move played for the time");
		}

		final PlayerInfo playerInfo = this.players.get(historyEntry.player);
		playerInfo.rack.removeAll(historyEntry.drawn);
		historyEntry.metaInformation.getFilledSquares().forEach(
				filled -> playerInfo.rack.add(filled.stone)
		);
		this.grid.remove(historyEntry.metaInformation);
		this.players.forEach( (p,info) -> info.score += historyEntry.scores.getOrDefault(p, 0));
		playerInfo.score -= historyEntry.metaInformation.getScore();
		if (!this.toPlay.isEmpty())
		{
			assert this.toPlay.peekLast() == historyEntry.player;
			this.toPlay.removeLast();
		}
		this.toPlay.addFirst(historyEntry.player);
		LOGGER.info("Last move rollback on demand of " + caller);
		this.state.set(State.STARTED);
		dispatch(toInform -> toInform.afterRollback());
		dispatch(toInform -> toInform.onPlayRequired(historyEntry.player));

		this.waitingForPlay.countDown();
	}

	@Override
	public synchronized AbstractPlayer getPlayerToPlay()
	{
		return this.toPlay.getFirst();
	}

	/**
	 * Ends the game.
	 *
	 * @param firstEndingPlayer player which has first emptied its rack.
	 */
	private synchronized void endGame(PlayerInfo firstEndingPlayer, final HistoryEntry historyEntry)
	{
		this.state.set(State.ENDED);
		final StringBuffer message = new StringBuffer();
		message.append(firstEndingPlayer.getName()).append(" has cleared its rack.\n");
		this.players.forEach(
				(player, info) ->
				{
					if (info != firstEndingPlayer)
					{
						int gift = 0;
						for (final Stone stone : info.rack)
						{
							gift += stone.getPoints();
						}
						info.score -= gift;
						firstEndingPlayer.score += gift;
						historyEntry.scores.put(player, -gift);
						historyEntry.scores.put(firstEndingPlayer.player, historyEntry.scores.get(firstEndingPlayer.player) + gift);
						message.append(info.getName()).append(" gives ").append(gift).append(" points\n");
					}
				});

		dispatch(c -> c.onDispatchMessage(message.toString()));
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
			throw new Error(new ScrabbleException(ScrabbleException.ERROR_CODE.FORBIDDEN, "No such player"));
		}
		return info;
	}

	/**
	 * Refill the rack of a player.
	 *
	 * @param player Player to refill the rack
	 * @return the new drawn stones.
	 */
	private Set<Stone> refillRack(final AbstractPlayer player)
	{
		final Rack rack = this.players.get(player).rack;
		final HashSet<Stone> drawn = new HashSet<>();
		while (!this.bag.isEmpty() && rack.size() < RACK_SIZE)
		{
			final Stone stone = this.bag.poll();
			this.bag.remove(stone);
			drawn.add(stone);
			rack.add(stone);
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
			throw new ScrabbleException(ScrabbleException.ERROR_CODE.ASSERTION_FAILED, "Cannot start game: no player registered");
		}

		prepareGame();

		this.state.set(State.STARTED);
		this.moveNr = 1;
		try
		{
			do
			{
				if (this.state.get() == State.ENDED)
				{
					// let the players the possibility to rollback
					this.waitingForPlay = new CountDownLatch(1);
					this.waitingForPlay.await(10L, TimeUnit.SECONDS);
					if (this.state.get() == State.ENDED)
					{
						break;
					}
				}
				else
				{
					final AbstractPlayer player = this.toPlay.peekFirst();
					if (player == null)
					{
						throw new AssertionError();
					}
					LOGGER.info("Let's play " + player);
					this.waitingForPlay = new CountDownLatch(1);
					dispatch(p -> p.onPlayRequired(player));
					this.waitingForPlay.await();
				}

			} while (true);
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
			throw new ScrabbleException(ScrabbleException.ERROR_CODE.PLAYER_IS_OBSERVER);
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
		this.state.set(State.ENDED);
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
			throw new ScrabbleException(ScrabbleException.ERROR_CODE.NOT_IDENTIFIED);
		}
	}

	State getState()
	{
		return this.state.get();
	}

	private static class PlayerInfo implements IPlayerInfo
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
	}

	/**
	 * State of the game
	 */
	enum  State
	{BEFORE_START, STARTED, ENDED}

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
		 */
		void onPlayRequired(AbstractPlayer currentPlayer);

		void onDictionaryChange();

		void onDispatchMessage(String msg);

		void afterRollback();

		void afterPlay(final int moveNr, final IPlayerInfo info, final IAction action, final int score);

		void beforeGameStart();

		void afterGameEnd();

		Queue<ScrabbleEvent> getIncomingEventQueue();
	}

	/**
	 * Default listener. Does nothing.
	 */
	public static class DefaultGameListener implements GameListener
	{

		private final LinkedList<ScrabbleEvent> queue = new LinkedList<>();

		@Override
		public void onPlayRequired(final AbstractPlayer currentPlayer)
		{
		}

		@Override
		public void onDictionaryChange()
		{
		}

		@Override
		public void onDispatchMessage(final String msg)
		{
		}

		@Override
		public void afterRollback()
		{
		}

		@Override
		public void afterPlay(final int moveNr, final IPlayerInfo info, final IAction action, final int score)
		{
		}

		@Override
		public void beforeGameStart()
		{
		}

		@Override
		public void afterGameEnd()
		{
		}

		@Override
		public Queue<ScrabbleEvent> getIncomingEventQueue()
		{
			return this.queue;
		}
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
		private AbstractPlayer player;
		private final IAction action;
		private final boolean errorOccurred;
		private final HashMap<AbstractPlayer, Integer> scores = new HashMap<>();
		private final Set<Stone> drawn;  // to be used for rewind

		/**
		 * Information about the move at time of the action.
		 */
		private final Grid.MoveMetaInformation metaInformation;

		private HistoryEntry(final AbstractPlayer player, final IAction action, final boolean errorOccurred, final int score, final Set<Stone> drawn, final Grid.MoveMetaInformation metaInformation)
		{
			this.player = player;
			this.action = action;
			this.errorOccurred = errorOccurred;
			this.scores.put(player, score);
			this.drawn = drawn;
			this.metaInformation = metaInformation;
		}

		public String print()
		{
			final StringBuilder sb = new StringBuilder(this.player.getName());
			sb.append(" - ").append(this.errorOccurred ? "*" : "").append(((Move) this.action).getNotation());
			sb.append(" ").append(this.scores).append(" pts");
			return sb.toString();
		}
	}
}
