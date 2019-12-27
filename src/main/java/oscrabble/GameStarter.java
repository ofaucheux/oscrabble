package oscrabble;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.BasicConfigurator;
import oscrabble.client.SwingClient;
import oscrabble.dictionary.Dictionary;
import oscrabble.player.BruteForceMethod;
import oscrabble.server.Game;

import javax.swing.*;
import java.io.File;

public class GameStarter
{
	public static void main(String[] args)
	{
		BasicConfigurator.configure();
		final Game game = new Game(Dictionary.getDictionary(Dictionary.Language.FRENCH));

		try
		{
			final File file = new File("C:\\temp\\scrabble.properties");
			FileUtils.touch(file);

			final Dictionary dictionary = game.getDictionary();
			final BruteForceMethod method = new BruteForceMethod(dictionary);
			game.addPlayer(method.new Player("Computer"));
			game.addPlayer(method.new Player("Computer2"));
			game.addPlayer(new SwingClient("Olivier"));
			game.addPlayer(new SwingClient("Pascal"));
			game.startGame();
		}
		catch (final Throwable e)
		{
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, e, "Error occurred", JOptionPane.ERROR_MESSAGE);
		}
	}
}
