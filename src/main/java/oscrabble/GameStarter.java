package oscrabble;

import org.apache.commons.configuration2.builder.fluent.Parameters;
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
	public static void main(String[] args) throws ScrabbleException
	{
		BasicConfigurator.configure();
		final Game game = new Game(Dictionary.getDictionary(Dictionary.Language.FRENCH));

		try
		{
			final File file = new File("C:\\temp\\scrabble.properties");
			FileUtils.touch(file);

			final Dictionary dictionary = game.getDictionary();
			final BruteForceMethod method = new BruteForceMethod(dictionary);
			game.register(method.new Player("Computer"));
			game.register(method.new Player("Computer2"));
			game.register(new SwingClient("Swing"));
			game.startGame();
		}
		catch (final Throwable e)
		{
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, e, "Error occurred", JOptionPane.ERROR_MESSAGE);
		}
	}
}
