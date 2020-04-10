package oscrabble;

import org.apache.log4j.Logger;
import oscrabble.client.SwingPlayer;
import oscrabble.dictionary.Dictionary;
import oscrabble.player.BruteForceMethod;
import oscrabble.server.Game;

import javax.swing.*;

public class GameStarter
{
	/** Logger */
	public static final Logger LOGGER = Logger.getLogger(GameStarter.class);

	public static void main(String[] args)
	{
		Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> LOGGER.fatal("Uncaught exception", throwable));

		try
		{
			final Game game;
			final Dictionary dictionary = Dictionary.getDictionary(Dictionary.Language.FRENCH);
			game = new Game(Game.DEFAULT_PROPERTIES_FILE);
			if (game.getPlayers().isEmpty())
			{
				game.addPlayer(new SwingPlayer("Emil"));
				game.addPlayer(new BruteForceMethod(dictionary).new Player("R2D2"));
			}
			game.play();
		}
		catch (final Throwable e)
		{
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, e, "Error occurred", JOptionPane.ERROR_MESSAGE);
		}
	}

}
