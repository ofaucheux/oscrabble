package oscrabble.dictionary;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.swing.*;

class DictionaryComponentTest
{

	@Test
	@Disabled("swing tests are disabled")
	public void test() throws InterruptedException
	{
		final JFrame dictionaryFrame = new JFrame();
		final DictionaryComponent dico = new DictionaryComponent(new UnMotDotNet());
		dictionaryFrame.add(dico);
		dictionaryFrame.setVisible(true);
		dictionaryFrame.setSize(300, 200);
		dictionaryFrame.setLocationRelativeTo(null);

		dico.showDescription("Repas");

		do
		{
			Thread.sleep(100);
		} while (dictionaryFrame.isVisible());
	}
}