package oscrabble.dictionary;

import org.apache.log4j.Logger;
import org.junit.jupiter.api.Test;

import javax.swing.*;

import java.awt.*;

public class WikitionaryTest
{

	public static final Logger LOGGER = Logger.getLogger(WikitionaryTest.class);

	@Test
	public void getDescription() throws InterruptedException, DictionaryException
	{
		final int width = 200;
		final Wikitionary wikitionary = new Wikitionary("https://fr.wiktionary.org");
		wikitionary.setHtmlWidth(width);
		Iterable<String> description = wikitionary.getDefinitions("ahaner");
		LOGGER.info(description);

		final JFrame jFrame = new JFrame();
		final ScrollPane sp = new ScrollPane();
		jFrame.add(sp);
		sp.add(new JLabel(String.valueOf(description)));
		jFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		jFrame.setSize(width*2, 400);
		jFrame.setLocationRelativeTo(null);

		jFrame.setVisible(true);
		Thread.sleep(1000);
		jFrame.dispose();
	}
}