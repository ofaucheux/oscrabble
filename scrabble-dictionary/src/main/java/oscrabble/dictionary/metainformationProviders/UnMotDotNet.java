package oscrabble.dictionary.metainformationProviders;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.dictionary.DictionaryException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class UnMotDotNet implements WordMetainformationProvider {
	private static final Logger LOGGER = LoggerFactory.getLogger(UnMotDotNet.class);

	@Override
	public List<String> getDefinitions(String word) throws DictionaryException {
		try {
			word = word.toLowerCase(Locale.FRANCE);
			word = word.replaceAll("[éèê]", "e");  // todo: auch a. u usw?

			final ArrayList<String> definitions = new ArrayList<>(4);

			final String url = "http://1mot.net/" + word;
			final Document doc = Jsoup.connect(url).get();
			LOGGER.trace("Get document from " + url);
			final ArrayList<Paragraph> paragraphs = new ArrayList<>();
			final Elements h4s = doc.getElementsByTag("h4");

			for (final Element h4 : h4s) {
				final Paragraph paragraph = new Paragraph();
				paragraph.title = h4.ownText();
				Element el = h4;
				while ((el = el.nextElementSibling()) != null && !el.tag().normalName().equals("h4")) {
					if (el.tag().normalName().equals("ul")) {
						for (final Element il : el.getElementsByTag("li")) {
							paragraph.texts.add(il.wholeText());
						}
					}
				}
				paragraphs.add(paragraph);
			}

			final Pattern extraitParagraphTitle = Pattern.compile("courts? extrait?");
			for (final Paragraph paragraph : paragraphs) {
				if (extraitParagraphTitle.matcher(paragraph.title).find()) {
					definitions.addAll(paragraph.texts);
				}
			}
			return definitions;
		} catch (final Throwable e) {
			final String message = "No definition found for " + word;
			LOGGER.error(message, e);
			throw new DictionaryException(message);
		}
	}

	private static class Paragraph {
		String title;
		List<String> texts = new ArrayList<>();

		@Override
		public String toString() {
			return this.title;
		}
	}
}
