package oscrabble;

import oscrabble.client.SwingPlayer;
import oscrabble.dictionary.Dictionary;
import oscrabble.player.AbstractPlayer;
import oscrabble.player.BruteForceMethod;
import oscrabble.server.Game;

import javax.swing.*;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class  GameStarter
{
	public static void main(String[] args)
	{
		try
		{
			final Properties allProperties = new Properties();

			final File propertyFile = new File("C:\\temp\\scrabble.properties");
			try (FileReader reader = new FileReader(propertyFile))
			{
				allProperties.load(reader);
			}

			final String language = allProperties.getProperty("LANGUAGE", "FRENCH");
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

			final Pattern keyPart = Pattern.compile("([^.]*)");
			final Set<String> playerNames = new HashSet<>();
			getSubProperties(allProperties, "player").stringPropertyNames().forEach(k ->
			{
				 Matcher m = keyPart.matcher(k);
				if (m.matches())
				{
					playerNames.add(m.group(1));
				}
			});

			for (final String name : playerNames)
			{
				final Properties playerProps = getSubProperties(allProperties, name);
				final AbstractPlayer player;
				final String methodName = playerProps.getProperty("method");
				switch (PlayerType.valueOf(methodName.toUpperCase()))
				{
					case SWING:
						player = new SwingPlayer(name);
						break;
					case BRUTE_FORCE:
						player = new BruteForceMethod(dictionary).new Player(name);
//						player.loadConfiguration()
						break;
					default:
						throw new ConfigurationException("Unknown method: " + methodName);
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
	 * Return a set of the properties which keys start with a prefix. The keys in the new set don't contain the prefix anymore.
	 *
	 * @param source original property set
	 * @param prefix searched prefix
	 * @return the new porperty set.
	 */
	private static Properties getSubProperties(final Properties source, final String prefix)
	{
		final Properties result = new Properties();
		final Pattern keyPattern = Pattern.compile(Pattern.quote(source + ".") + "(.*)");
		for (final String key : source.stringPropertyNames())
		{
			final Matcher m = keyPattern.matcher(key);
			if (m.matches())
			{
				result.setProperty(m.group(1), source.getProperty(key));
			}
		}
		return result;
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
