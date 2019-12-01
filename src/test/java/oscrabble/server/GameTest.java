package oscrabble.server;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import oscrabble.Grid;
import oscrabble.Move;
import oscrabble.ScrabbleException;
import oscrabble.dictionary.Dictionary;

import java.text.ParseException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class GameTest
{

	private Game game;
	private Grid grid;
	private TestPlayer gustav;
	private TestPlayer john;
	private TestPlayer jurek;

	@BeforeEach
	public void initialize()
	{
		final Dictionary FRENCH_DICTIONARY = Dictionary.getDictionary(Dictionary.Language.FRENCH);

		this.game = new Game(FRENCH_DICTIONARY, -3300078916872261882L);
		this.grid = this.game.getGrid();

		this.gustav = new TestPlayer("Gustav", this.game);
		this.john = new TestPlayer("John", this.game);
		this.jurek = new TestPlayer("Jurek", this.game);
		this.game.register(this.gustav);
		this.game.register(this.john);
		this.game.register(this.jurek);
	}

	@After
	public void endsGame()
	{
//		this.game.ends(); // TODO: to be implemented
	}

	@Test
	void completeGame() throws ScrabbleException, ParseException
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
				new Game.DefaultGameListener()
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

		this.game.startGame();

		assertFalse(this.grid.getSquare("8K").isEmpty());
		assertFalse(this.grid.getSquare("8L").isEmpty());
		assertFalse(this.grid.getSquare("8M").isEmpty());
		this.game.rollbackLastMove(null);
		Assert.assertTrue(this.grid.getSquare("8K").isEmpty());
		assertFalse(this.grid.getSquare("8L").isEmpty());
		Assert.assertTrue(this.grid.getSquare("8M").isEmpty());

		this.game.getPlayerInfo(this.john).getScore();
		assertFalse(this.grid.getSquare("N10").isEmpty());
		this.game.rollbackLastMove(null);
		Assert.assertTrue(this.grid.getSquare("N10").isEmpty());


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
		this.startGame(true);
		this.gustav.addMove(Move.parseMove(this.grid, "H3 APPETEE"));
		assertEquals(this.game.getScore(this.gustav), 0);
		assertNotEquals(this.gustav, this.game.getPlayerToPlay());
	}

	/**
	 * Start the game.
	 *
	 * @param fork on a new thread if true.
	 */
	public void startGame(final boolean fork) throws ScrabbleException, InterruptedException
	{
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
