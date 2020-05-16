package oscrabble;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.server.Game;

import javax.swing.*;

public class GameStarter
{
	/** Logger */
	public static final Logger LOGGER = LoggerFactory.getLogger(GameStarter.class);

	public static void main(String[] args)
	{
		Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> LOGGER.error("Uncaught exception", throwable));

		try
		{
			final Game game;
//			final IDictionary dictionary = new MicroServiceDictionary(Language.FRENCH);
			game = new Game(Game.DEFAULT_PROPERTIES_FILE);
			if (game.getPlayers().isEmpty())
			{
				// TODO
//				game.addPlayer(new SwingPlayer("Emil"));
//				game.addPlayer(new BruteForceMethod(game.getDictionary()).new Player("R2D2"));
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
