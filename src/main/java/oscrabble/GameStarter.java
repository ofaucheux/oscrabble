package oscrabble;

import oscrabble.client.SwingPlayer;
import oscrabble.dictionary.Dictionary;
import oscrabble.player.BruteForceMethod;
import oscrabble.server.Game;

import javax.swing.*;
import java.util.Random;

public class  GameStarter
{
	public static void main(String[] args)
	{
		try
		{
			final Game game;
			if (Game.DEFAULT_PROPERTIES_FILE.exists())
			{
				game = Game.fromProperties(null);
			}
			else
			{
				final Dictionary dictionary = Dictionary.getDictionary(Dictionary.Language.FRENCH);
				game = new Game(dictionary, new Random().nextLong());
				game.addPlayer(new SwingPlayer("Emil"));
				game.addPlayer(new BruteForceMethod(dictionary).new Player("R2D2"));
			}
			game.startGame();
		}
		catch (final Throwable e)
		{
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, e, "Error occurred", JOptionPane.ERROR_MESSAGE);
		}
	}

}
