package oscrabble;

import oscrabble.client.SwingClient;
import oscrabble.dictionary.Dictionary;
import oscrabble.player.AbstractPlayer;
import oscrabble.player.BruteForceMethod;
import oscrabble.server.Game;

import javax.swing.*;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class  GameStarter
{
	public static void main(String[] args)
	{
		try
		{
			final Properties properties = new Properties();

			final File propertyFile = new File("C:\\temp\\scrabble.properties");
			try (FileReader reader = new FileReader(propertyFile))
			{
				properties.load(reader);
			}

			final String language = properties.getProperty("LANGUAGE", "FRENCH");
			Dictionary dictionary;
			try
			{
				dictionary = Dictionary.getDictionary(Dictionary.Language.valueOf(language));
			}
			catch (IllegalArgumentException e)
			{
				throw new ConfigurationException("Not known language: " + language);
			}
			final Game game = new Game(dictionary);

			//
			// Players
			//

			final HashMap<String, HashMap<String, String>> players = new HashMap<>();
			final Pattern keyPattern = Pattern.compile("player\\.([^.]+)\\.(.*)");
			for (final String key : properties.stringPropertyNames())
			{
				final Matcher m = keyPattern.matcher(key);
				if (m.matches())
				{
					final String name = m.group(1);
					players.putIfAbsent(name, new HashMap<>());
					players.get(name).put(m.group(2), properties.getProperty(key));
				}
			}

			for (Map.Entry<String, HashMap<String, String>> entry : players.entrySet())
			{
				String name = entry.getKey();
				HashMap<String, String> playerProps = entry.getValue();
				final AbstractPlayer player;
				final String method = playerProps.get("method");
				switch (PlayerType.valueOf(method.toUpperCase()))
				{
					case SWING:
						player = new SwingClient(name);
						break;
					case BRUTE_FORCE:
						player = new BruteForceMethod(dictionary).new Player(name);
						break;
					default:
						throw new ConfigurationException("Unknown method: " + method);
				}
				game.addPlayer(player);
			}

			game.startGame();
		}
		catch (final Throwable e)
		{
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, e, "Error occurred", JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Problem occurring while reading / writing the configuration.
	 */
	private static final class ConfigurationException extends ScrabbleException
	{
		/**
		 * Constructor
		 * @param message message to display.
		 */
		private ConfigurationException(final String message)
		{
			super(message);
		}
	}

	/** Player types */
	public enum PlayerType
	{
		SWING,
		BRUTE_FORCE
	}

}
