package oscrabble.server;

import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.HashBag;
import org.apache.commons.collections4.bag.TreeBag;
import org.apache.commons.collections4.bidimap.DualLinkedHashBidiMap;
import org.apache.log4j.Logger;
import oscrabble.*;
import oscrabble.client.Exchange;
import oscrabble.dictionary.Dictionary;
import oscrabble.player.AbstractPlayer;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class ScrabbleServer implements IScrabbleServer
{
	private final static Logger LOGGER = Logger.getLogger(ScrabbleServer.class);


	public final static int RACK_SIZE = 7;
	static final String SCRABBLE_MESSAGE = "Scrabble!";

	private final DualLinkedHashBidiMap<AbstractPlayer, PlayerInfo> players = new DualLinkedHashBidiMap<>();
	private final LinkedList<AbstractPlayer> toPlay = new LinkedList<>();
	private final Grid grid;
	private final Random random;
	private CountDownLatch waitingForPlay;
	final LinkedList<Stone> bag = new LinkedList<>();
	private final Dictionary dictionary;

	/** State of the game */
	private State state;

	/** Parameter of the server */
	private final ServerConfiguration configuration = new ServerConfiguration();

	ScrabbleServer(final Dictionary dictionary, final Random random)
	{
		this.dictionary = dictionary;
		this.grid = new Grid(this.dictionary);
		this.random = random;
		this.waitingForPlay = new CountDownLatch(1);

		this.state = State.BEFORE_START;
	}

	public ScrabbleServer(final Dictionary dictionary)
	{
		this (dictionary, new Random());
	}

	@Override
	public synchronized void register(final AbstractPlayer newPlayer)
	{
		final PlayerInfo info = new PlayerInfo();
		info.player = newPlayer;
		info.key = UUID.randomUUID();
		newPlayer.setPlayerKey(info.key);
		info.rack = new Rack();
		info.eventQueue = new LinkedBlockingDeque<>();
		info.dispatchThread = new Thread(() -> {
			try
			{
				while (true)
				{
					final ScrabbleEvent event = info.eventQueue.poll(Integer.MAX_VALUE, TimeUnit.DAYS);
					assert event != null;
					event.dispatch(info.player);
				}
			}
			catch (InterruptedException e)
			{
				reportError(info, e);
			}
		});
		this.players.put(newPlayer, info);

		info.dispatchThread.start();
		if (!newPlayer.isObserver())
		{
			this.toPlay.push(newPlayer);
		}

	}


	/**
	 * Report an error coming from a client
	 *
	 * @param info
	 * @param e
	 */
	private void reportError(final PlayerInfo info, final InterruptedException e)
	{
		// TODO
		System.out.println(e);
	}



	@Override
	public synchronized int play(final AbstractPlayer player, final IAction action)
	{
		assertIsCurrentlyPlaying(player);

		final PlayerInfo playerInfo = this.players.get(player);
		boolean done = false;
		try
		{
			int score = 0;
			final ArrayList<String> messages = new ArrayList<>(5);

			if (action instanceof Move)
			{
				final Move move = (Move) action;

				// check possibility
				final Grid.MoveMetaInformation moveMI = this.grid.getMetaInformation(move);
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
					if (!move.getSquares().keySet().contains(center))
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
			}
			else
			{
				throw new AssertionError("Command not treated: " + action);
			}

			refillRack(player);
			dispatch(toInform -> toInform.afterPlay(playerInfo, action, 0));
			messages.forEach(m -> dispatchMessage(m));

			LOGGER.debug("Grid after play\n" + this.grid.asASCIIArt());

			done = true;
			return score;
		}
		catch (final ScrabbleException e)
		{
			LOGGER.info("Refuse play: " + e);
			player.onDispatchMessage(e.toString());
			if (this.configuration.acceptNewAttemptAfterForbiddenMove || e.acceptRetry())
			{
				player.onDispatchMessage("Retry accepted");
				done = false;
			}
			else
			{
				dispatchMessage("Player " + player + " would have play an illegal move: " + e + ". Skip its turn");
				done = true;
			}
			return 0;
		}
		finally
		{
			if (playerInfo.rack.isEmpty())
			{
				endGame(playerInfo);
			}
			else
			{
				if (done)
				{
					this.toPlay.pop();
					this.toPlay.add(player);
				}
				this.waitingForPlay.countDown();
			}
		}
	}

	/**
	 * Ends the game.
	 * @param firstEndingPlayer player which has first emptied its rack.
	 */
	private synchronized void endGame(PlayerInfo firstEndingPlayer)
	{
		this.state = State.ENDED;
		this.toPlay.clear();
		final StringBuffer message = new StringBuffer();
		message.append(firstEndingPlayer.getName()).append(" hat clear its rack.\n");
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
						message.append(info.getName()).append(" gives ").append(gift).append(" points\n");
					}
				});

		dispatch(c -> c.onDispatchMessage(message.toString()));
	}

	/**
	 * Send a message to all the clients.
	 * @param message message to dispatch
	 */
	private void dispatchMessage(final String message)
	{
		this.players.keySet().forEach(p -> p.onDispatchMessage(message));
	}

	private void assertIsCurrentlyPlaying(final AbstractPlayer player)
	{
		if (player != this.toPlay.getFirst())
		{
			throw new IllegalStateException("The player " + player.toString() + " is not playing");
		}
	}

	@Override
	public List<IPlayerInfo> getPlayers()
	{
		return List.copyOf(this.players.values());
	}


	private void refillRack(final AbstractPlayer player)
	{
		final Rack rack = this.players.get(player).rack;
		while (!this.bag.isEmpty() && rack.size() < RACK_SIZE)
		{
			final Stone stone = this.bag.poll();
			this.bag.remove(stone);
			rack.add(stone);
		}
		LOGGER.trace("Remaining stones in the bag: " + this.bag.size());
	}

	@Override
	public void markAsIllegal(final String word)
	{
		this.getDictionary().markAsIllegal(word);
		dispatch(AbstractPlayer::onDictionaryChange);
	}

	public void startGame()
	{
		prepareGame();

		try
		{
			do
			{
				state = State.STARTED;
				final AbstractPlayer player = this.toPlay.peekFirst();
				LOGGER.info("Let's play " + player);
				this.waitingForPlay = new CountDownLatch(1);
				dispatch( p -> p.onPlayRequired(player));
				if (this.waitingForPlay.await(1, TimeUnit.MINUTES))
				{
					// OK
				}
				else
				{
					// TODO: timeout
				}
				Thread.sleep(50);
			} while (this.state != State.ENDED);
		}
		catch (InterruptedException e)
		{
			LOGGER.error(e, e);
		}

		dispatch(AbstractPlayer::afterGameEnd);
	}

	public void prepareGame()
	{
		assert !this.toPlay.isEmpty();

		fillBag();
		sortPlayers();

		// Fill racks
		for (final AbstractPlayer player : this.toPlay)
		{
			refillRack(player);
		}

		dispatch(player -> player.beforeGameStart());
	}

	/**
	 * Sortiert (oder mischt) die Spieler, um eine Spielreihenfolge zu definieren.
	 */
	private void sortPlayers()
	{
		// select the player playing the first
		Collections.shuffle(this.toPlay);
	}

	/**
	 * Füllt das Säckchen mit den Buchstaben.
	 */
	void fillBag()
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
	 * Send an event at all the players.
	 */
	private void dispatch(final ScrabbleEvent scrabbleEvent)
	{
		for (final AbstractPlayer player : this.players.keySet())
		{
			LOGGER.trace("Send " + scrabbleEvent + " to " + player.getName());
			scrabbleEvent.dispatch(player);
		}
	}

	@Override
	public Dictionary getDictionary()
	{
		return this.dictionary;
	}

	@Override
	public Grid getGrid()
	{
		return this.grid;
	}


	@Override
	public Rack getRack(final AbstractPlayer player, final UUID clientKey) throws ScrabbleException
	{
		checkKey(player, clientKey);
		if (player.isObserver())
		{
			throw new ScrabbleException(ScrabbleException.ERROR_CODE.PLAYER_IS_OBSERVER);
		}
		return this.players.get(player).rack.copy();
	}

	@Override
	public int getScore(final AbstractPlayer player)
	{
		return this.players.get(player).getScore();
	}

	@Override
	public boolean hasEditableParameters(final UUID caller, final IPlayerInfo player)
	{
		return this.players.getKey(player).hasEditableParameters();
	}

	@Override
	public void editParameters(final UUID caller, final IPlayerInfo player)
	{
		this.players.getKey(player).editParameters();
	}

	/**
	 * @return the parameter of the server. TODO: made them editable only by master of game.
	 */
	public ServerConfiguration getConfiguration()
	{
		return this.configuration;
	}

	private void checkKey(final AbstractPlayer player, final UUID clientKey) throws ScrabbleException
	{
		if (clientKey == null || !clientKey.equals(this.players.get(player).key))
		{
			throw new ScrabbleException(ScrabbleException.ERROR_CODE.NOT_IDENTIFIED);
		}
	}

	private static class PlayerInfo implements IPlayerInfo
	{
		AbstractPlayer player;

		/**
		 * Password für die Kommunikation Player &lt;&gt; Server
		 */
 		UUID key;
 		BlockingQueue<ScrabbleEvent> eventQueue;
		Thread dispatchThread;
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
	}

	/** State of the game */
	private enum State { BEFORE_START, STARTED, ENDED}

	public interface ScrabbleEvent
	{
		void dispatch(final AbstractPlayer player);
	}
}
