package oscrabble.dictionary;

import org.apache.log4j.Logger;

import java.awt.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UnMotDotNet implements WordMetainformationProvider
{
	private static final Logger LOGGER = Logger.getLogger(UnMotDotNet.class);
	public static final Pattern PATTERN_PARTICIPE_PASSE = Pattern.compile("(.*)ée?s?");

	@Override
	public String getDescription(final String word)
	{
		final ArrayList<String> searchWords = new ArrayList<>();
		searchWords.add(word);
		final Matcher m = PATTERN_PARTICIPE_PASSE.matcher(word);
		if (m.matches())
		{
			searchWords.add(m.group(1) + "er");
		}

		try
		{
			URL url = null;
			assert searchWords.size() > 0;
			for (final String searchWord : searchWords)
			{
				url = new URL("https://1mot.net/" + searchWord);
				HttpURLConnection connection = null;
				try
				{
					connection = (HttpURLConnection) url.openConnection();
					connection.setRequestMethod("GET");
					connection.connect();
					if (connection.getResponseCode() != HttpURLConnection.HTTP_NOT_FOUND)
					{
						break;
					}
				}
				finally
				{
					if (connection != null)
					{
						connection.disconnect();
					}
				}
			}

			if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
			{
				Desktop.getDesktop().browse(url.toURI());
			}
		}
		catch (final URISyntaxException | IOException e)
		{
			LOGGER.error("Cannot find " + word, e);
		}
		return null;
	}
}
