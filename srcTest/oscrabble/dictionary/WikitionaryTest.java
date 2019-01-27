package oscrabble.dictionary;

import org.junit.jupiter.api.Test;

import javax.swing.*;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

class WikitionaryTest
{

	@Test
	void getDescription() throws InterruptedException
	{
		final int width = 200;
		final Wikitionary wikitionary = new Wikitionary("https://fr.wiktionary.org");
		wikitionary.setHtmlWidth(width);
		String description = wikitionary.getDescription("ahaner");
		System.out.println(description);

		final JFrame jFrame = new JFrame();
		final ScrollPane sp = new ScrollPane();
		jFrame.add(sp);
		sp.add(new JLabel(description));
		jFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		jFrame.setSize(width*2, 400);
		jFrame.setLocationRelativeTo(null);

		jFrame.setVisible(true);
		while (jFrame.isVisible())
		{
			Thread.sleep(500);
		}
	}
}