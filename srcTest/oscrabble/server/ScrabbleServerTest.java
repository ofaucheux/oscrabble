package oscrabble.server;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.spi.RootLogger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import oscrabble.Move;
import oscrabble.dictionary.Dictionary;
import oscrabble.client.SwingClient;
import oscrabble.player.AbstractPlayer;
import oscrabble.player.BruteForceMethod;
import oscrabble.player.ConsolePlayer;

class ScrabbleServerTest
{
	@BeforeAll
	public static void log4j()
	{
		BasicConfigurator.configure();
		RootLogger.getRootLogger().setLevel(Level.ALL);
	}

	@Test
	void markAsIllegal()
	{
		// TODO
	}

	@Test
	void getDictionary()
	{
		// TODO
	}

	@Test
	void getScore()
	{
		// TODO
	}

	@Test
	void play()
	{
		final Dictionary french = Dictionary.getDictionary(Dictionary.Language.FRENCH);
		final ScrabbleServer server = new ScrabbleServer(french)
		{
			@Override
			void fillBag()
			{
				final String letters = "orchidee" + "glooiwp" + " appeera";
				for (final char c : letters.toCharArray())
				{
					this.bag.add(french.generateStone(c));
				}
			}
		};

		final TestPlayer playerA = new TestPlayer("Player A", server);
		final TestPlayer playerB = new TestPlayer("Player B", server);
		server.register(playerA);
		server.register(playerB);


		server.startGame();
		playerA.setNextMove(new Move(server.getGrid().getSquare(7,7), Move.Direction.HORIZONTAL, "ORCHIDEE"));
	}

}