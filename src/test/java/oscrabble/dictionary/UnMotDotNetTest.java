package oscrabble.dictionary;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

class UnMotDotNetTest
{


	@Test
	void getDescription() throws DictionaryException
	{
		final UnMotDotNet wmip = new UnMotDotNet();
		final ArrayList<String> words = new ArrayList<>();
//		words.add("doxologies");
		words.add("cabanée"); // this word is only known of 1mot.net as the base verb "cabaner"

		for (final String word : words)
		{
			final List<String> descriptions = wmip.getDefinitions(word);
			for (final String description : descriptions)
			{
				final JLabel label = new JLabel(description);
				label.setSize(400, 0);
				JOptionPane.showMessageDialog(null, label);
			}
		}

	}


}