package oscrabble;

import org.apache.log4j.BasicConfigurator;
import oscrabble.client.SwingClient;
import oscrabble.dictionary.Dictionary;
import oscrabble.player.BruteForceMethod;
import oscrabble.server.ScrabbleServer;

import javax.swing.*;

public class GameStarter
{
	public static void main(String[] args)
	{
		BasicConfigurator.configure();

		try
		{
			final ScrabbleServer server = new ScrabbleServer(Dictionary.getDictionary(Dictionary.Language.FRENCH));
			final Dictionary dictionary = server.getDictionary();
			final BruteForceMethod method = new BruteForceMethod(dictionary);
			server.register(method.new Player(server, "Computer"));
			server.register(method.new Player(server, "Computer2"));
			server.register(new SwingClient(server, "Swing"));
			server.startGame();
		}
		catch (final Throwable e)
		{
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, e, "Error occurred", JOptionPane.ERROR_MESSAGE);
		}
	}
}
