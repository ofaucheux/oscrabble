package oscrabble.server;

import org.apache.commons.collections4.Bag;
import org.junit.Assert;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.ScrabbleException;
import oscrabble.controller.MicroServiceDictionary;
import oscrabble.data.GameState;
import oscrabble.data.objects.Grid;
import oscrabble.dictionary.Application;
import oscrabble.dictionary.Language;
import oscrabble.controller.Action;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class GameTest
{

	public static final Logger LOGGER = LoggerFactory.getLogger(GameTest.class);
	public static final Random RANDOM = new Random();
	public static MicroServiceDictionary DICTIONARY = new MicroServiceDictionary(URI.create("http://localhost:8080"), "FRENCH");
	private static Application application;

	private Game game;
	private Grid grid;
	private PredefinedPlayer gustav;
	private PredefinedPlayer john;
	private PredefinedPlayer jurek;

	@BeforeAll
	public static void mocken()
	{
//		application = new Application();
//		application.start();
		DICTIONARY = new MicroServiceDictionary(
				URI.create("http://localhost:8080/"),
				Language.FRENCH.toString()
		);

		Thread.setDefaultUncaughtExceptionHandler((t,e) -> LOGGER.error(e.toString(), e));
	}

	@AfterAll
	public static void stop()
	{
		if (application != null)
		{
			application.stop();
		}
	}

	@BeforeEach
	public void initialize() throws ScrabbleException
	{
		this.game = new Game(DICTIONARY, -3300078916872261882L);
		this.game.randomPlayerOrder = false;

		this.grid = this.game.getGrid();
		final int gameNr = RANDOM.nextInt(100);

		this.gustav = addPlayer("Gustav_" + gameNr);
		this.john = addPlayer("John_" + gameNr);
		this.jurek = addPlayer("Jurek_" + gameNr);
	}

	/**
	 * Create and add a player in the game
	 * @param name name of the player
	 * @return this player
	 */
	private PredefinedPlayer addPlayer(final String name) throws ScrabbleException
	{
		final PredefinedPlayer player = new PredefinedPlayer(this.game, name);
		return this.game.addPlayer(player);
	}

	@AfterEach
	public void endsGame()
	{
		this.game.quitGame();
	}

//	@Test TODO
//	void completeRandomGame() throws InterruptedException, ScrabbleException
//	{
//		for (int gameNr = 0; gameNr < 10; gameNr++)
//		{
//			this.game = new Game(DICTIONARY);
//			this.game.delayBeforeEnds = 0;
//			final BruteForceMethod method = new BruteForceMethod(DICTIONARY);
//			this.game.listeners.add(new TestListener()
//			{
//				@Override
//				public void afterPlay(final Action action)
//				{
//					LOGGER.info("Played: " + action.toString());
//				}
//			});
//
//			ArrayList<Game.Player> players = new ArrayList<>();
//			for (int i = 0; i < RandomUtils.nextInt(1, 7); i++)
//			{
//				final BruteForceMethod.Player player = method.new Player("Player " + i)
//				{
//					{
//						final Configuration configuration = this.getConfiguration();
//						configuration.setValue("throttle", 0);
//						configuration.setValue("force", 100);
//						configuration.setValue("strategy", BruteForceMethod.Strategy.BEST_SCORE);
//					}
//				};
//
//				this.game.addPlayer(player);
////				players.add(player);
//			}
//			final AtomicReference<Throwable> error = new AtomicReference<>();
//			this.game.listeners.add(new TestListener()
//			{
//
////				@Override TODO
////				public void afterRejectedAction(final AbstractPlayer player, final Action action)
////				{
////					Assert.fail("Rejected action: " + action);
////				}
//
////				@Override
////				public void afterPlay(final Action action)
////				{
////						if (RANDOM.nextInt(10) == 0)
////						{
////							try
////							{
////								Game.Player caller = players.get(0);
//////								UUID key = getKey(caller);
////								final Snapshot before = this.snapshots.getLast();
////								assert before != null;
////								GameTest.this.game.rollbackLastMove(caller);
////								final Snapshot after = collectInfos();
////								assertEquals("Wrong play nr", before.roundNr, after.roundNr);
////								before.scores.forEach(
////										(player, beforeScore) -> assertEquals("Wrong score", beforeScore, after.scores.get(player))
////								);
////							}
////							catch (final Throwable e)
////							{
////								error.set(e);
////							}
////						}
////						else
////						{
////							this.snapshots.add(collectInfos());
////						}
////				}
//
////				protected Snapshot collectInfos()
////				{
////					final Snapshot info = new Snapshot();
////					info.lastPlay = GameTest.this.game.actions.getLast();
////					info.roundNr = GameTest.this.game.getRoundNr();
////					for (final IPlayerInfo player : GameTest.this.game.getPlayers())
////					{
////						info.scores.put(player.getName(), player.getScore());
////					}
////					return info;
////				}
//
//			});
//			startGame(true);
//
//			while (this.game.getState() != GameState.State.ENDED)
//			{
//				Thread.sleep(100);
//			}
//
//			assertNull(error.get());
//		}
//	}

	// TODO ?
//	/**
//	 * Get the key of a player through reflection methods
//	 * @param player the player
//	 * @return the key
//	 */
//	protected UUID getKey(final BruteForceMethod.Player player)
//	{
//		try
//		{
//			Field field = AbstractPlayer.class.getDeclaredField("playerKey");
//			field.setAccessible(true);
//			return (UUID) field.get(player);
//		}
//		catch (NoSuchFieldException | IllegalAccessException e)
//		{
//			throw new Error(e);
//		}
//	}

	/**
	 * TODO: enable it again. For this, define the order of the rack.
	 * @throws ScrabbleException
	 * @throws InterruptedException
	 * @throws TimeoutException
	 */
	@Test
	@Disabled
	void completeKnownGame() throws ScrabbleException, InterruptedException, TimeoutException
	{
		final List<PredefinedPlayer> players = Arrays.asList(this.gustav, this.john, this.jurek);
		final LinkedList<String> moves = new LinkedList<>(Arrays.asList(
				/*  1 */ "H3 APPETES",
				/*  2 */ "G9 VIGIE",
				/*  3 */ "7C WOmBATS",
				/*  4 */ "3G FATIGUE",
				/*  5 */ "12A DETELAI",
				/*  6 */ "8A ABUS",
				/*  7 */ "13G ESTIMAIT",
				/*  8 */ "5G EPErONNA",
				/*  9 */ "O3 ECIMER",
				/* 10 */ "D3 KOUROS",
				/* 11 */ "L8 ECHOUA",
				/* 12 */ "3A FOLKS",
				/* 13 */ "A1 DEFUNT",
				/* 14 */ "1A DRAYOIR",
				/* 15 */ "L2 QUAND",
				/* 16 */ "1A DRAYOIRE",
				/* 17 */ "11I ENJOUE",
				/* 18 */ "B10 RIELS",
				/* 19 */ "N10 VENTA",
				/* 20 */ "8K HEM"
		));

		for (int i = 0; i < moves.size(); i++)
		{
			players.get(i % players.size()).moves.add(moves.get(i));
		}

		this.game.listeners.add(
				new TestListener()
				{
					@Override
					public void afterPlay(final Action play)
					{
						switch (GameTest.this.game.getRoundNr())
						{
							case 1:
								Assert.assertEquals(78, GameTest.this.gustav.score);
								break;
						}
					}
				}
		);

		startGame(true);
		this.game.awaitEndOfPlay(moves.size());

		// first rollback
		assertFalse(this.grid.isEmpty("8K"));

		assertFalse(this.grid.isEmpty("8K"));
		assertFalse(this.grid.isEmpty("8L"));
		assertFalse(this.grid.isEmpty("8M"));
		this.game.rollbackLastMove(null);
		assertTrue(this.grid.isEmpty("8K"));
		assertFalse(this.grid.isEmpty("8L"));
		assertTrue(this.grid.isEmpty("8M"));
		assertEquals(this.john, this.game.getPlayerToPlay());

		// second rollback
		assertFalse(this.grid.isEmpty("N10"));
		this.game.rollbackLastMove(null);
		assertTrue(this.grid.isEmpty("N10"));
		assertEquals(this.gustav, this.game.getPlayerToPlay());

		// play both last moves again
		this.gustav.moves.add("N10 VENTA");
		this.john.moves.add("8K HEM");
		this.game.awaitEndOfPlay(moves.size());
		assertEquals(GameState.State.ENDED, this.game.getState());

		Thread.sleep(this.game.delayBeforeEnds * 5000 / 2 + 500);
		assertEquals(GameState.State.ENDED, this.game.getState());
	}

	@Test
	public void notAcceptedWord() throws ScrabbleException, InterruptedException, TimeoutException
	{
		this.game = new Game(DICTIONARY);  // for having only one player
		this.game.assertFirstLetters("FTINOA ");

		final PredefinedPlayer etienne = addPlayer("Etienne");
		startGame(true);

		etienne.moves.add("G8 As");
		this.game.awaitEndOfPlay(1);
		assertFalse(etienne.isLastPlayError);

		etienne.moves.add("8H SIF");
		this.game.awaitEndOfPlay(2);
		assertTrue(etienne.isLastPlayError);
	}

	@Test
	public void retryAccepted() throws ScrabbleException, InterruptedException, TimeoutException
	{
		// test retry accepted
		this.grid = this.game.getGrid();
		this.game.getConfiguration().setValue("retryAccepted", true);
		this.game.assertFirstLetters("APPESTEE");
		this.startGame(true);
		assertEquals(0, this.game.getRoundNr());

		this.gustav.moves.add("H3 APPETQE");
		Thread.sleep(100);
		assertEquals(this.game.getScore(this.gustav), 0);
		assertEquals(this.gustav, this.game.getPlayerToPlay());
		assertEquals(0, this.game.getRoundNr());

		this.gustav.moves.add("8H APTES");
		this.game.awaitEndOfPlay(1);
		assertNotEquals(this.gustav, this.game.getPlayerToPlay());
		assertEquals(16, this.game.getScore(this.gustav));
	}

	@Test
	public void rollback() throws ScrabbleException, InterruptedException, TimeoutException
	{
		this.grid = this.game.getGrid();
		this.game.assertFirstLetters( "APTESSIF");
		this.startGame(true);

		final int roundNr = this.game.getRoundNr();
		final Bag<Character> startRack = this.gustav.rack;
		this.gustav.moves.add("8H APTES");
		this.game.awaitEndOfPlay(1);
		assertEquals(16, this.game.getScore(this.gustav));
		assertNotEquals(this.gustav, this.game.getPlayerToPlay());

		this.game.rollbackLastMove(this.gustav);
		assertEquals(roundNr, this.game.getRoundNr());
		assertEquals(0, this.game.getScore(this.gustav));
		assertEquals(this.gustav, this.game.getPlayerToPlay());
		assertEquals(startRack, this.gustav.rack);
	}


	@Test
	public void retryForbidden() throws ScrabbleException, InterruptedException, TimeoutException
	{
		this.game.getConfiguration().setValue("retryAccepted", false);
		final AtomicBoolean playRejected = new AtomicBoolean(false);
		final TestListener listener = new TestListener()
		{
			@Override
			public void afterRejectedAction(final Game.Player player, final Action action){}
			{
				playRejected.set(true);
			}
		};
		this.game.listeners.add(listener);
		this.startGame(true);
		this.gustav.moves.add("H3 APPETEE");
		this.game.awaitEndOfPlay(1);

		assertTrue(playRejected.get());
		assertEquals(this.game.getScore(this.gustav), 0);
		assertNotEquals(this.gustav, this.game.getPlayerToPlay());
	}

	@Test
	public void startWithOnlyOneLetter() throws ScrabbleException, InterruptedException
	{
		this.game.getConfiguration().setValue("retryAccepted", false);
		this.game.assertFirstLetters("A");
		startGame(true);
		this.gustav.moves.add("H8 A");
		Thread.sleep(100);
		assertTrue(this.gustav.isLastPlayError);
		assertNotEquals(this.gustav, this.game.getPlayerToPlay());
	}


	@Test
	public void startNotCentered() throws ScrabbleException, InterruptedException, TimeoutException
	{
		this.game.getConfiguration().setValue("retryAccepted", false);
		startGame(true);
		this.gustav.moves.add("G7 AS");
		this.game.awaitEndOfPlay(1);
		assertTrue(this.gustav.isLastPlayError);
		assertNotEquals(this.gustav, this.game.getPlayerToPlay());
	}

	@Test
	public void wordDoesNotTouch() throws ScrabbleException, InterruptedException, TimeoutException
	{
		this.game.getConfiguration().setValue("retryAccepted", false);
		this.game.assertFirstLetters("ASWEEDVIGIE");
		startGame(true);
		this.gustav.moves.add("H8 AS");
		this.game.awaitEndOfPlay(1);
		assertFalse(this.gustav.isLastPlayError);
		this.john.moves.add("A3 VIGIE");
		Thread.sleep(100);
		assertTrue(this.john.isLastPlayError);
	}

	@Test
	public void testScore() throws ScrabbleException, InterruptedException, TimeoutException
	{
		this.game = new Game(DICTIONARY, 2346975568742590367L);
		this.game.assertFirstLetters("FTINOA ");

		final PredefinedPlayer etienne = addPlayer("Etienne");
		startGame(true);
		final Grid grid = this.game.getGrid();

		etienne.moves.add("H8 As");
		this.game.awaitEndOfPlay(1);
		assertEquals(2, this.game.getScore(etienne));

		etienne.moves.add("8I SI");
		this.game.awaitEndOfPlay(2);
		assertEquals(4, this.game.getScore(etienne));

//		do
//		{
//			this.game = new Game(FRENCH_DICTIONARY);
//			final TestPlayer p2 = new TestPlayer("Anton", this.game);
//			this.game.addPlayer(p2);
//			startGame(true);
//			final Rack rack = this.game.getRack(p2, p2.getKey());
//			final List<Character> characters = rack.getCharacters();
//			if (characters.contains(' '))
//			{
//				System.out.println("Rand: " + game.randomSeed + " - Chars: " + characters);
//			}
//			this.game.quitGame();
//		} while (true);
//
		{
			// Joker on normal case
			this.game = new Game(DICTIONARY);
			final PredefinedPlayer anton = addPlayer("anton");
			this.game.assertFirstLetters(" CELMNPIERA");

			startGame(true);
			int move = 1;
			anton.moves.add("D8 PLaCE");
			this.game.awaitEndOfPlay(move++);
			assertEquals(22, this.game.getScore(anton));
			anton.moves.add(RANDOM.nextBoolean() ? "4F NIERa" : "4F NIERA");
			this.game.awaitEndOfPlay(move);
			assertEquals(28, this.game.getScore(anton));
			this.game.quitGame();
		}

		{
			// Joker on blue case
			this.game = new Game(DICTIONARY, -6804219371477742897L);
			final PredefinedPlayer anton = addPlayer("Anton");
			this.game.assertFirstLetters(" CELMNPAISSE");
			startGame(true);
			int move = 1;
			anton.moves.add("D8 aMPLE");
			this.game.awaitEndOfPlay(move++);
			assertEquals(14, this.game.getScore(anton));
			anton.moves.add("7D CAISSE");
			this.game.awaitEndOfPlay(move);
			assertEquals(28, this.game.getScore(anton));
			this.game.quitGame();
		}
	}

//	private void setRack(final Game.Player player, final String tiles)
//	{
//		player.rack.clear();
//		for (final char c : tiles.toCharArray())
//		{
//			player.rack.add(c);
//		}
//	}

	/**
	 * Start the game.
	 *
	 * @param fork on a new thread if true.
	 */
	public void startGame(final boolean fork) throws ScrabbleException, InterruptedException
	{
		this.game.listeners.add(new TestListener()
		{
			@Override
			public void onGameStateChanged()
			{
				LOGGER.info("Game state changed to " + GameTest.this.game.getState().name());
			}
		});
		if (fork)
		{
			final AtomicReference<ScrabbleException> exception = new AtomicReference<>();
			new Thread(() -> this.game.play()).start();
			Thread.sleep(300);
			if (exception.get() != null)
			{
				throw exception.get();
			}
		}
		else
		{
			this.game.play();
		}

	}

	/**
	 * A player playing pre-defined turns.
	 */
	private class PredefinedPlayer extends Game.Player
	{
		final ArrayBlockingQueue<String> moves = new ArrayBlockingQueue<>(1024);

		private final ArrayBlockingQueue<ScrabbleEvent> scrabbleEvents = new ArrayBlockingQueue<>(1024);

		private final AbstractGameListener listener;

		PredefinedPlayer(final Game game, final String name)
		{
			super(name);
			this.id = name;
			this.listener = new AbstractGameListener()
			{

				@Override
				public Queue<ScrabbleEvent> getIncomingEventQueue()
				{
					return PredefinedPlayer.this.scrabbleEvents;
				}

				@Override
				public void onPlayRequired(final Game.Player player)
				{
					if (player == PredefinedPlayer.this)
						try
						{
							GameTest.this.game.play(PredefinedPlayer.this, Action.parse(PredefinedPlayer.this.moves.poll(60, TimeUnit.SECONDS)));
						}
						catch (ScrabbleException | InterruptedException e)
						{
							throw new Error(e);
						}
				}
			};

			game.addListener(this.listener);
			final Thread th = new Thread(() ->
			{
				try
				{
					while (true)
					{
						final ScrabbleEvent event = this.scrabbleEvents.poll(1, TimeUnit.MINUTES);
						if (event == null)
						{
							throw new AssertionError("No input from server");
						}
						event.accept(this.listener);
					}
				}
				catch (InterruptedException e)
				{
					throw new Error(e);
				}
			});

			th.setName("Player Thread - " + this.name);
			th.setDaemon(true);
			th.start();
		}
	}
}

/**
 * Default listener. Does nothing.
 */
abstract class TestListener implements GameListener
{

	private final ArrayBlockingQueue<ScrabbleEvent> queue = new ArrayBlockingQueue<>(8);
	private final AtomicReference<Throwable> thrownException;

	TestListener()
	{
		this.thrownException = new AtomicReference<>();
		final Thread thread = new Thread(() -> {
			try
			{
				while (true)
				{
					final ScrabbleEvent event;
					event = TestListener.this.queue.take();
					event.accept(this);
				}
			}
			catch (final Throwable e)
			{
				this.thrownException.set(e);
			}
		});
		thread.setDaemon(true);
		thread.setName("Test listener");
		thread.start();
	}

	@Override
	public Queue<ScrabbleEvent> getIncomingEventQueue()
	{
		return this.queue;
	}

}

