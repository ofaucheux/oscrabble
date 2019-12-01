package oscrabble.server;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import oscrabble.Grid;
import oscrabble.Move;
import oscrabble.ScrabbleException;
import oscrabble.dictionary.Dictionary;

import java.text.ParseException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class GameTest
{

	@Test
	void play() throws ScrabbleException, ParseException
	{
		final TestEnvironment env = new TestEnvironment();
		final Game server = env.game;

		final List<TestPlayer> players = Arrays.asList(env.gustav, env.john, env.jurek);
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
		final Grid grid = server.getGrid();
		for (int i = 0; i < moves.size(); i++)
		{
			players.get(i % players.size()).addMove(
					Move.parseMove(grid, moves.get(i))
			);
		}

		server.listeners.add(
				new Game.DefaultGameListener()
				{
					@Override
					public void afterPlay(final int moveNr, final IPlayerInfo info, final IAction action, final int score)
					{
						switch (moveNr)
						{
							case 1:
								Assert.assertEquals(78, server.getPlayerInfo(env.gustav).getScore());
								break;
						}
					}
				}
		);

		server.startGame();

		Assert.assertFalse(grid.getSquare("8K").isEmpty());
		Assert.assertFalse(grid.getSquare("8L").isEmpty());
		Assert.assertFalse(grid.getSquare("8M").isEmpty());
		server.rollbackLastMove(null);
		Assert.assertTrue(grid.getSquare("8K").isEmpty());
		Assert.assertFalse(grid.getSquare("8L").isEmpty());
		Assert.assertTrue(grid.getSquare("8M").isEmpty());

		server.getPlayerInfo(env.john).getScore();
		Assert.assertFalse(grid.getSquare("N10").isEmpty());
		server.rollbackLastMove(null);
		Assert.assertTrue(grid.getSquare("N10").isEmpty());


	}

	@Test
	public void errorsCases() throws ScrabbleException, ParseException
	{
		final TestEnvironment env = new TestEnvironment();

		final Grid grid = env.game.getGrid();
		env.game.getConfiguration().setValue("retryAccepted", true);
		env.gustav.addMove(Move.parseMove(grid, "H3 APPETEE"));
		env.gustav.addMove(Move.parseMove(grid, "H3 APPETES"));
		env.game.startGame();
	}

	/**
	 * Vordefinierte Spiel Startumgebung.
	 */
	private static class TestEnvironment
	{
		public static final Dictionary FRENCH_DICTIONARY = Dictionary.getDictionary(Dictionary.Language.FRENCH);

		private Game game;
		private TestPlayer gustav;
		private TestPlayer john;
		private TestPlayer jurek;

		public TestEnvironment()
		{
			this.game = new Game(FRENCH_DICTIONARY, -3300078916872261882L);

			this.gustav = new TestPlayer("Gustav", this.game);
			this.john = new TestPlayer("John", this.game);
			this.jurek = new TestPlayer("Jurek", this.game);
			this.game.register(this.gustav);
			this.game.register(this.john);
			this.game.register(this.jurek);
		}
	}
}