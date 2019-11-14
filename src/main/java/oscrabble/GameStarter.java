package oscrabble;

import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.HierarchicalConfigurationXMLReader;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.ConfigurationBuilder;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.BasicConfigurator;
import oscrabble.client.SwingClient;
import oscrabble.dictionary.Dictionary;
import oscrabble.player.BruteForceMethod;
import oscrabble.server.ScrabbleServer;

import javax.swing.*;
import java.io.File;
import java.io.IOError;

public class GameStarter
{
	public static void main(String[] args)
	{
		BasicConfigurator.configure();
		new Game().start();
	}

	public static class Game
	{

		private ConfigurationBuilder<? extends FileBasedConfiguration> configBuilder;
		private ScrabbleServer server;

		public void start()
		{

			try
			{
				Parameters params = new Parameters();
				final File file = new File("C:\\temp\\scrabble.properties");
				FileUtils.touch(file);
//				configBuilder = new File<>(PropertiesConfiguration.class)
//						.configure(params.fileBased()
//								.setFile(file));
//				final FileBasedConfiguration configuration = configBuilder.getConfiguration();

				server = new ScrabbleServer(Dictionary.getDictionary(Dictionary.Language.FRENCH));
				final Dictionary dictionary = server.getDictionary();
				final BruteForceMethod method = new BruteForceMethod(dictionary);
				server.register(method.new Player(this, "Computer"));
				server.register(method.new Player(this, "Computer2"));
				server.register(new SwingClient(this, "Swing"));
				server.startGame();
			}
			catch (final Throwable e)
			{
				e.printStackTrace();
				JOptionPane.showMessageDialog(null, e, "Error occurred", JOptionPane.ERROR_MESSAGE);
			}
		}

		public void saveConfig()
		{
//			try
//			{
//				this.configBuilder.save();
//			}
//			catch (ConfigurationException e)
//			{
//				throw new IOError(e);
//			}
		}

		public ScrabbleServer getServer()
		{
			return this.server;
		}
	}
}

