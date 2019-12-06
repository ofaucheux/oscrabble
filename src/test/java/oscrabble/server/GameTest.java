package oscrabble.server;

import org.apache.commons.lang3.RandomUtils;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import oscrabble.Grid;
import oscrabble.Move;
import oscrabble.ScrabbleException;
import oscrabble.configuration.Configuration;
import oscrabble.dictionary.Dictionary;
import oscrabble.player.AbstractPlayer;
import oscrabble.player.BruteForceMethod;

import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
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

		this.gustav = new TestPlayer("Gustav " + gameNr, this.game);
		this.john = new TestPlayer("John " + gameNr, this.game);
		this.jurek = new TestPlayer("Jurek " + gameNr, this.game);
		this.game.register(this.gustav);
		this.game.register(this.john);
		this.game.register(this.jurek);
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

			for (int i = 0; i < RandomUtils.nextInt(1, 7); i++)
			{
				final BruteForceMethod.Player player = method.new Player("Player " + i)
				{
					{
						final Configuration configuration = this.configuration;
						configuration.setValue("throttle", 0);
						configuration.setValue("force", 100);
						configuration.setValue("strategy", BruteForceMethod.Strategy.BEST_SCORE);
					}
				};

				this.game.register(player);
			}
			this.game.listeners.add(new TestListener()
			{
				@Override
				public void afterRejectedAction(final AbstractPlayer player, final IAction action)
				{
					Assert.fail("Rejected action: " + action);
				}
			});
			startGame(true);

			while (this.game.getState() != Game.State.ENDED)
			{
				Thread.sleep(100);
			}
		}
	}

	@Test
	void completeKnownGame() throws ScrabbleException, ParseException, InterruptedException
	{
		final List<TestPlayer> players = Arrays.asList(this.gustav, this.john, this.jurek);
		final LinkedList<String> moves = new LinkedList<>(Arrays.asList(
				"H3 APPETES",
				"G9 VIGIE",
				"7C WOmBATS",
				"3G FATIGUE",
				"12A DETELAI",
				"8A ABUS",
				"13G ESTIMAIT",
				"5G EPErONNA",
				"O3 ECIMER",
				"D3 KOUROS",
				"L8 ECHOUA",
				"3A FOLKS",
				"A1 DEFUNT",
				"1A DRAYOIR",
				"L2 QUAND",
				"1A DRAYOIRE",
				"11I ENJOUE",
				"B10 RIELS",
				"N10 VENTA",
				"8K HEM"));
		for (int i = 0; i < moves.size(); i++)
		{
			players.get(i % players.size()).addMove(
					Move.parseMove(this.grid, moves.get(i))
			);
		}

		this.game.listeners.add(
				new TestListener()
				{
					@Override
					public void afterPlay(final int moveNr, final IPlayerInfo info, final IAction action, final int score)
					{
						switch (moveNr)
						{
							case 1:
								Assert.assertEquals(78, GameTest.this.game.getPlayerInfo(GameTest.this.gustav).getScore());
								break;
						}
					}
				}
		);

		startGame(true);

		assertFalse(this.grid.getSquare("8K").isEmpty());
		assertFalse(this.grid.getSquare("8L").isEmpty());
		assertFalse(this.grid.getSquare("8M").isEmpty());
		this.game.rollbackLastMove(null, null);
		Assert.assertTrue(this.grid.getSquare("8K").isEmpty());
		assertFalse(this.grid.getSquare("8L").isEmpty());
		Assert.assertTrue(this.grid.getSquare("8M").isEmpty());

		this.game.getPlayerInfo(this.john).getScore();
		assertFalse(this.grid.getSquare("N10").isEmpty());
		this.game.rollbackLastMove(null, null);
		Assert.assertTrue(this.grid.getSquare("N10").isEmpty());

		final int moveNr = this.game.moveNr;
		this.gustav.addMove(Move.parseMove(this.grid, "N10 VENTA"));
		this.john.addMove(Move.parseMove(this.grid, "8K HEM"));

		game.waitUntilMove(moveNr + 2);
		assertEquals(Game.State.ENDING, this.game.getState());

		Thread.sleep(this.game.delayBeforeEnds * 1000 / 2 + 500);
		assertEquals(Game.State.ENDED, this.game.getState());
	}

	@Test
	public void retryAccepted() throws ScrabbleException, ParseException, InterruptedException
	{
		// test retry accepted
		this.grid = this.game.getGrid();
		this.game.getConfiguration().setValue("retryAccepted", true);
		this.startGame(true);
		this.gustav.addMove(Move.parseMove(this.grid, "H3 APPETEE"));
		assertEquals(this.game.getScore(this.gustav), 0);
		assertEquals(this.gustav, this.game.getPlayerToPlay());
		this.gustav.addMove(Move.parseMove(this.grid, "H3 APTES"));
	}

	@Test
	public void retryForbidden() throws ScrabbleException, ParseException, InterruptedException
	{
		this.game.getConfiguration().setValue("retryAccepted", false);
		final AtomicBoolean playRejected = new AtomicBoolean(false);
		final TestListener listener = new TestListener()
		{
			@Override
			public void afterRejectedAction(final AbstractPlayer player, final IAction action)
			{
				playRejected.set(true);
			}
		};
		this.game.listeners.add(listener);
		this.startGame(true);
		this.gustav.addMove(Move.parseMove(this.grid, "H3 APPETEE"));
		this.game.waitUntilMove(1);

		assertTrue(playRejected.get());
		assertEquals(this.game.getScore(this.gustav), 0);
		assertNotEquals(this.gustav, this.game.getPlayerToPlay());
	}

	@Test
	public void startWithOnlyOneLetter() throws ScrabbleException, ParseException, InterruptedException
	{
		this.game.getConfiguration().setValue("retryAccepted", false);
		startGame(true);
		this.gustav.addMove(Move.parseMove(this.grid, "H8 A"));
		Thread.sleep(100);
		assertTrue(this.game.isLastPlayError(this.gustav));
		assertNotEquals(this.gustav, this.game.getPlayerToPlay());
	}


	@Test
	public void startNotCentered() throws ScrabbleException, ParseException, InterruptedException
	{
		this.game.getConfiguration().setValue("retryAccepted", false);
		startGame(true);
		this.gustav.addMove(Move.parseMove(this.grid, "G7 AS"));
		this.game.waitUntilMove(1);
		assertTrue(this.game.isLastPlayError(this.gustav));
		assertNotEquals(this.gustav, this.game.getPlayerToPlay());
	}

	@Test
	public void wordDoesNotTouch() throws ScrabbleException, ParseException, InterruptedException
	{
		this.game.getConfiguration().setValue("retryAccepted", false);
		startGame(true);
		this.gustav.addMove(Move.parseMove(this.grid, "H8 AS"));
		this.game.waitUntilMove(1);
		assertFalse(this.game.isLastPlayError(this.gustav));
		this.john.addMove(Move.parseMove(this.grid, "A3 VIGIE"));
		Thread.sleep(100);
		assertTrue(this.game.isLastPlayError(this.john));
	}

	@Test
	public void testScore() throws ScrabbleException, InterruptedException, ParseException
	{
		// dieser seed gibt die Buchstaben "[F, T, I, N, O, A,  - joker - ]"
		this.game = new Game(FRENCH_DICTIONARY, 2346975568742590367L);
		final TestPlayer p = new TestPlayer("Etienne", this.game);
		this.game.register(p);
		startGame(true);
		final Grid grid = this.game.getGrid();

		p.addMove(Move.parseMove(grid, "H7 As"));
		Thread.sleep(100);
		assertEquals(2, this.game.getScore(p));

		p.addMove(Move.parseMove(grid, "8H SI"));
		Thread.sleep(100);
		assertEquals(3, this.game.getScore(p));
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
			new Thread(() -> {
				try
				{
					this.game.startGame();
				}
				catch (ScrabbleException e)
				{
					exception.set(e);
				}
			}).start();
			Thread.sleep(300);
			if (exception.get() != null)
			{
				throw exception.get();
			}
		}
		else
		{
			this.game.startGame();
		}

	}

}

/**
 * Default listener. Does nothing.
 */
class TestListener implements Game.GameListener
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

