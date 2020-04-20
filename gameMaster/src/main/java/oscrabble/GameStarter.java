package oscrabble;

import org.apache.log4j.Logger;
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
//			final IDictionary dictionary = new MicroServiceDictionary(Language.FRENCH);
			game = new Game(Game.DEFAULT_PROPERTIES_FILE);
			if (game.getPlayers().isEmpty())
			{
//				game.addPlayer(new SwingPlayer("Emil"));
				game.addPlayer(new BruteForceMethod(game.getDictionary()).new Player("R2D2"));
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
