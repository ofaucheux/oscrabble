package oscrabble.dictionary;

import org.apache.log4j.Logger;

import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

public class UnMotDotNet implements WordMetainformationProvider
{
	private static final Logger LOGGER = Logger.getLogger(UnMotDotNet.class);

	@Override
	public String getDescription(final String word)
	{
		// https://en.wiktionary.org/w/index.php?title=test&printable=yes
		try
		{
			final URL url = new URL("http://1mot.net/" + word);
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
