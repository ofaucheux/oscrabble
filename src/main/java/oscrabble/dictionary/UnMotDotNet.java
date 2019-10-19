package oscrabble.dictionary;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UnMotDotNet implements WordMetainformationProvider
{
	private static final Logger LOGGER = Logger.getLogger(UnMotDotNet.class);

	@Override
	public List<String> getDefinitions(String word) throws DictionaryException
	{
		try
		{
			word = word.toLowerCase(Locale.FRANCE);
			word = word.replaceAll("[éèê]", "e");
			final Document doc  = Jsoup.connect("http://1mot.net/" + word).get();
			final Elements els = doc.getElementsByAttributeValue("class", "md");
			final ArrayList<String> definitions = new ArrayList<>(4);
			for (final Element el : els)
			{
				String text = el.text();
				if (text.startsWith("•"))
				{
					definitions.add("<html>" + text);
				}
			}
			return definitions;
		}
		catch (final Throwable e)
		{
			final String message = "No definition found for " + word;
			LOGGER.error(message, e);
			throw new DictionaryException(message);
		}
	}
}
