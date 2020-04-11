package oscrabble.server;

import org.apache.commons.lang3.RandomUtils;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import oscrabble.Grid;
import oscrabble.Rack;
import oscrabble.ScrabbleException;
import oscrabble.action.Action;
import oscrabble.action.PlayTiles;
import oscrabble.configuration.Configuration;
import oscrabble.dictionary.Dictionary;
import oscrabble.player.AbstractPlayer;
import oscrabble.player.BruteForceMethod;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class GameTest
{

	public static final Logger LOGGER = Logger.getLogger(GameTest.class);
	public static final Random RANDOM = new Random();

	private Game game;
	private Grid grid;
	private TestPlayer gustav;
	private TestPlayer john;
	private TestPlayer jurek;
	public static final Dictionary FRENCH_DICTIONARY = Dictionary.getDictionary(Dictionary.Language.FRENCH);

	@BeforeEach
	public void initialize()
	{

		this.game = new Game(FRENCH_DICTIONARY, -3300078916872261882L);
		this.grid = this.game.getGrid();
		final int gameNr = RANDOM.nextInt(100);

		this.gustav = new TestPlayer("Gustav_" + gameNr, this.game);
		this.john = new TestPlayer("John_" + gameNr, this.game);
		this.jurek = new TestPlayer("Jurek_" + gameNr, this.game);
		this.game.addPlayer(this.gustav);
		this.game.addPlayer(this.john);
		this.game.addPlayer(this.jurek);
	}

	@AfterEach
	public void endsGame()
	{
		this.game.quitGame();
	}

	@Test
	void completeRandomGame() throws InterruptedException, ScrabbleException
	{
		for (int gameNr = 0; gameNr < 10; gameNr++)
		{
			this.game = new Game(FRENCH_DICTIONARY);
			this.game.delayBeforeEnds = 0;
			final BruteForceMethod method = new BruteForceMethod(FRENCH_DICTIONARY);
			this.game.listeners.add(new TestListener()
			{
				@Override
				public void afterPlay(final Play play)
				{
					LOGGER.info("Played: " + play.action.toString());
				}
			});

			ArrayList<BruteForceMethod.Player> players = new ArrayList<>();
			for (int i = 0; i < RandomUtils.nextInt(1, 7); i++)
			{
				final BruteForceMethod.Player player = method.new Player("Player " + i)
				{
					{
						final Configuration configuration = this.getConfiguration();
						configuration.setValue("throttle", 0);
						configuration.setValue("force", 100);
						configuration.setValue("strategy", BruteForceMethod.Strategy.BEST_SCORE);
					}
				};

				this.game.addPlayer(player);
				players.add(player);
			}
			final AtomicReference<Throwable> error = new AtomicReference<>();
			this.game.listeners.add(new TestListener()
			{
				final LinkedList<Snapshot> snapshots = new LinkedList<>();

				@Override
				public void afterRejectedAction(final AbstractPlayer player, final Action action)
				{
					Assert.fail("Rejected action: " + action);
				}

				@Override
				public void afterPlay(final Play play)
				{
						if (RANDOM.nextInt(10) == 0)
						{
							try
							{
								BruteForceMethod.Player caller = players.get(0);
								UUID key = getKey(caller);
								final Snapshot before = this.snapshots.getLast();
								assert before != null;
								GameTest.this.game.rollbackLastMove(caller, key);
								final Snapshot after = collectInfos();
								assertEquals("Wrong play nr", before.roundNr, after.roundNr);
								before.scores.forEach(
										(player, beforeScore) -> assertEquals("Wrong score", beforeScore, after.scores.get(player))
								);
							}
							catch (final Throwable e)
							{
								error.set(e);
							}
						}
						else
						{
							this.snapshots.add(collectInfos());
						}
				}

				protected Snapshot collectInfos()
				{
					final Snapshot info = new Snapshot();
					info.lastPlay = GameTest.this.game.plays.getLast();
					info.roundNr = GameTest.this.game.getRoundNr();
					for (final IPlayerInfo player : GameTest.this.game.getPlayers())
					{
						info.scores.put(player.getName(), player.getScore());
					}
					return info;
				}

				/**
				 * Infos about the game at a given point.
				 */
				class Snapshot
				{
					public Play lastPlay;
					public int roundNr;
					final HashMap<String, Integer> scores = new HashMap<>();
				}
			});
			startGame(true);

			while (this.game.getState() != Game.State.ENDED)
			{
				Thread.sleep(100);
			}

			assertNull(error.get());
		}
	}

	/**
	 * Get the key of a player through reflection methods
	 * @param player the player
	 * @return the key
	 */
	protected UUID getKey(final BruteForceMethod.Player player)
	{
		try
		{
			Field field = AbstractPlayer.class.getDeclaredField("playerKey");
			field.setAccessible(true);
			return (UUID) field.get(player);
		}
		catch (NoSuchFieldException | IllegalAccessException e)
		{
			throw new Error(e);
		}
	}

	@Test
	void completeKnownGame() throws ScrabbleException, ParseException, InterruptedException, TimeoutException
	{
		final List<TestPlayer> players = Arrays.asList(this.gustav, this.john, this.jurek);
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
			players.get(i % players.size()).addMove(
					PlayTiles.parseMove(this.grid, moves.get(i))
			);
		}

		this.game.listeners.add(
				new TestListener()
				{
					@Override
					public void afterPlay(final Play play)
					{
						switch (GameTest.this.game.getRoundNr())
						{
							case 1:
								Assert.assertEquals(78, GameTest.this.game.getPlayerInfo(GameTest.this.gustav).getScore());
								break;
						}
					}
				}
		);

		startGame(true);
		this.game.awaitEndOfPlay(moves.size(), 1, TimeUnit.MINUTES);

		// first rollback
		assertFalse(this.grid.getSquare("8K").isEmpty());
		assertFalse(this.grid.getSquare("8L").isEmpty());
		assertFalse(this.grid.getSquare("8M").isEmpty());
		this.game.rollbackLastMove(null, null);
		assertTrue(this.grid.getSquare("8K").isEmpty());
		assertFalse(this.grid.getSquare("8L").isEmpty());
		assertTrue(this.grid.getSquare("8M").isEmpty());
		assertEquals(this.john, this.game.getPlayerToPlay());

		// second rollback
		assertFalse(this.grid.getSquare("N10").isEmpty());
		this.game.rollbackLastMove(null, null);
		assertTrue(this.grid.getSquare("N10").isEmpty());
		assertEquals(this.gustav, this.game.getPlayerToPlay());

		// play both last moves again
		this.gustav.addMove(PlayTiles.parseMove(this.grid, "N10 VENTA"));
		this.john.addMove(PlayTiles.parseMove(this.grid, "8K HEM"));
		this.game.awaitEndOfPlay(moves.size(), 5, TimeUnit.SECONDS);
		assertEquals(Game.State.ENDED, this.game.getState());

		Thread.sleep(this.game.delayBeforeEnds * 5000 / 2 + 500);
		assertEquals(Game.State.ENDED, this.game.getState());
	}

	@Test
	public void retryAccepted() throws ScrabbleException, ParseException, InterruptedException, TimeoutException
	{
		// test retry accepted
		this.grid = this.game.getGrid();
		this.game.getConfiguration().setValue("retryAccepted", true);
		this.startGame(true);
		assertEquals(1, this.game.getRoundNr());

		this.gustav.addMove(PlayTiles.parseMove(this.grid, "H3 APPETEE"));
		Thread.sleep(100);
		assertEquals(this.game.getScore(this.gustav), 0);
		assertEquals(this.gustav, this.game.getPlayerToPlay());
		assertEquals(1, this.game.getRoundNr());

		this.gustav.addMove(PlayTiles.parseMove(this.grid, "8H APTES"));
		this.game.awaitEndOfPlay(1, 1, TimeUnit.SECONDS);
		assertNotEquals(this.gustav, this.game.getPlayerToPlay());
		assertEquals(this.game.getScore(this.gustav), 16);
	}

	@Test
	public void rollback() throws ScrabbleException, ParseException, InterruptedException, TimeoutException
	{
		this.grid = this.game.getGrid();
		this.startGame(true);
		final Rack startRack = ((Game.PlayerInfo) this.game.getPlayerInfo(this.gustav)).rack;
		this.gustav.addMove(PlayTiles.parseMove(this.grid, "8H APTES"));
		this.game.awaitEndOfPlay(1, 1, TimeUnit.SECONDS);
		assertEquals(16, this.game.getScore(this.gustav));
		assertNotEquals(this.gustav, this.game.getPlayerToPlay());

		this.game.rollbackLastMove(this.gustav, this.gustav.getKey());
		assertEquals(1, this.game.getRoundNr());
		assertEquals(0, this.game.getScore(this.gustav));
		assertEquals(this.gustav, this.game.getPlayerToPlay());
		assertEquals(startRack, ((Game.PlayerInfo) this.game.getPlayerInfo(this.gustav)).rack);
	}


	@Test
	public void retryForbidden() throws ScrabbleException, ParseException, InterruptedException, TimeoutException
	{
		this.game.getConfiguration().setValue("retryAccepted", false);
		final AtomicBoolean playRejected = new AtomicBoolean(false);
		final TestListener listener = new TestListener()
		{
			@Override
			public void afterRejectedAction(final AbstractPlayer player, final Action action)
			{
				playRejected.set(true);
			}
		};
		this.game.listeners.add(listener);
		this.startGame(true);
		this.gustav.addMove(PlayTiles.parseMove(this.grid, "H3 APPETEE"));
		this.game.awaitEndOfPlay(1, 1, TimeUnit.SECONDS);

		assertTrue(playRejected.get());
		assertEquals(this.game.getScore(this.gustav), 0);
		assertNotEquals(this.gustav, this.game.getPlayerToPlay());
	}

	@Test
	public void startWithOnlyOneLetter() throws ScrabbleException, ParseException, InterruptedException
	{
		this.game.getConfiguration().setValue("retryAccepted", false);
		startGame(true);
		this.gustav.addMove(PlayTiles.parseMove(this.grid, "H8 A"));
		Thread.sleep(100);
		assertTrue(this.game.isLastPlayError(this.gustav));
		assertNotEquals(this.gustav, this.game.getPlayerToPlay());
	}


	@Test
	public void startNotCentered() throws ScrabbleException, ParseException, InterruptedException, TimeoutException
	{
		this.game.getConfiguration().setValue("retryAccepted", false);
		startGame(true);
		this.gustav.addMove(PlayTiles.parseMove(this.grid, "G7 AS"));
		this.game.awaitEndOfPlay(1, 1, TimeUnit.SECONDS);
		assertTrue(this.game.isLastPlayError(this.gustav));
		assertNotEquals(this.gustav, this.game.getPlayerToPlay());
	}

	@Test
	public void wordDoesNotTouch() throws ScrabbleException, ParseException, InterruptedException, TimeoutException
	{
		this.game.getConfiguration().setValue("retryAccepted", false);
		startGame(true);
		this.gustav.addMove(PlayTiles.parseMove(this.grid, "H8 AS"));
		this.game.awaitEndOfPlay(1, 1, TimeUnit.SECONDS);
		assertFalse(this.game.isLastPlayError(this.gustav));
		this.john.addMove(PlayTiles.parseMove(this.grid, "A3 VIGIE"));
		Thread.sleep(100);
		assertTrue(this.game.isLastPlayError(this.john));
	}

	@Test
	public void testScore() throws ScrabbleException, InterruptedException, ParseException, TimeoutException
	{
		// dieser seed gibt die Buchstaben "[F, T, I, N, O, A,  - joker - ]"
		this.game = new Game(FRENCH_DICTIONARY, 2346975568742590367L);
		final TestPlayer p = new TestPlayer("Etienne", this.game);
		this.game.addPlayer(p);
		startGame(true);
		final Grid grid = this.game.getGrid();

		p.addMove(PlayTiles.parseMove(grid, "H7 As"));
		Thread.sleep(100);
		assertEquals(2, this.game.getScore(p));

		p.addMove(PlayTiles.parseMove(grid, "8H SI"));
		Thread.sleep(100);
		assertEquals(3, this.game.getScore(p));

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
			// Rand: -6804219371477742897 - Chars: [ , C, E, L, M, N, P]
			this.game = new Game(FRENCH_DICTIONARY, -6804219371477742897L);
			final TestPlayer anton = new TestPlayer("Anton", this.game);
			this.game.addPlayer(anton);
			startGame(true);
			int move = 1;
			anton.addMove(PlayTiles.parseMove(this.game.getGrid(), "8D PLaCE"));
			this.game.awaitEndOfPlay(move++, 1, TimeUnit.SECONDS);
			assertEquals(22, this.game.getScore(anton));
			anton.addMove(PlayTiles.parseMove(this.game.getGrid(),
					RANDOM.nextBoolean() ? "F4 NIERa" : "F4 NIERA"));
			this.game.awaitEndOfPlay(move, 1, TimeUnit.SECONDS);
			assertEquals(28, this.game.getScore(anton));
			this.game.quitGame();
		}

		{
			// Joker on blue case
			// Rand: -6804219371477742897 - Chars: [ , C, E, L, M, N, P]
			this.game = new Game(FRENCH_DICTIONARY, -6804219371477742897L);
			final TestPlayer anton = new TestPlayer("Anton", this.game);
			this.game.addPlayer(anton);
			startGame(true);
			int move = 1;
			anton.addMove(PlayTiles.parseMove(this.game.getGrid(), "8D aMPLE"));
			this.game.awaitEndOfPlay(move++, 1, TimeUnit.SECONDS);
			assertEquals(14, this.game.getScore(anton));
			anton.addMove(PlayTiles.parseMove(this.game.getGrid(), "D7 CAISSE"));
			this.game.awaitEndOfPlay(move, 1, TimeUnit.SECONDS);
			assertEquals(28, this.game.getScore(anton));
			this.game.quitGame();
		}
	}

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
				LOGGER.info("Game state changed to " + GameTest.this.game.getState());
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

}

/**
 * Default listener. Does nothing.
 */
abstract class TestListener implements Game.GameListener
{

	private final ArrayBlockingQueue<Game.ScrabbleEvent> queue = new ArrayBlockingQueue<>(8);
	private final AtomicReference<Throwable> thrownException;

	TestListener()
	{
		this.thrownException = new AtomicReference<>();
		final Thread thread = new Thread(() -> {
			try
			{
				while (true)
				{
					final Game.ScrabbleEvent event;
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
	public Queue<Game.ScrabbleEvent> getIncomingEventQueue()
	{
		return this.queue;
	}

}

