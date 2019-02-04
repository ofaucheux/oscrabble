package oscrabble.server;

import org.junit.jupiter.api.Test;
import oscrabble.Move;
import oscrabble.dictionary.Dictionary;
import oscrabble.dictionary.DictionaryTest;

import java.util.Random;

public class ScrabbleServerTest
{
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

	/**
	 * Baut einen Test-Server. Der erste Player bekommt die Buchstaben {@code EIUBO S}.
	 * @return
	 */
	public static ScrabbleServer getTestServer()
	{
		return new ScrabbleServer(DictionaryTest.getTestDictionary(), new Random(0));
	}

}