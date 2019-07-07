package oscrabble.server;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import oscrabble.Move;
import oscrabble.dictionary.Dictionary;
import oscrabble.dictionary.DictionaryTest;
import oscrabble.player.AbstractPlayer;

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
				final String letters = "orchidee" + "glooiwp" + "appeera";
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
		playerA.setNextMove(new Move(server.getGrid().getSquare(7, 7), Move.Direction.HORIZONTAL, "ORCHIDEE"));
	}

	/**
	 * Baut einen Test-Server. Der erste Player bekommt die Buchstaben {@code EIUBO S}.
	 * @return
	 */
	public static ScrabbleServer getTestServer()
	{
		return new ScrabbleServer(DictionaryTest.getTestDictionary(), new Random(0));
	}

	/**
	 * Check the scrabble-Message is send when a player a scrabble plays.
	 */
	@Test
	void scrabble()
	{
		final Dictionary french = Dictionary.getDictionary(Dictionary.Language.FRENCH);
		final String SCRABBLE_WORD = "RELIEUR";
		final ScrabbleServer server = new ScrabbleServer(french)
		{
			@Override
			void fillBag()
			{
				for (final char c : SCRABBLE_WORD.toCharArray())
				{
					this.bag.add(french.generateStone(c));
				}
			}
		};

		server.register(new AbstractPlayer("")
		{

			private boolean found;

			@Override
			public void onPlayRequired()
			{
				server.play(this, new Move(server.getGrid().getCenter(), Move.Direction.HORIZONTAL, SCRABBLE_WORD));
			}

			@Override
			public void onDictionaryChange()
			{}

			@Override
			public void onDispatchMessage(final String msg)
			{
				if (msg.equals(ScrabbleServer.SCRABBLE_MESSAGE))
				{
					this.found = true;
				}

				System.out.println(msg);
			}

			@Override
			public void afterPlay(final IPlayerInfo info, final IAction action, final int score)
			{}

			@Override
			public void beforeGameStart()
			{}

			@Override
			public boolean isObserver()
			{
				return false;
			}

			@Override
			public void afterGameEnd()
			{
				Assertions.assertTrue(this.found);
			}

		});

		server.startGame();
	}
}