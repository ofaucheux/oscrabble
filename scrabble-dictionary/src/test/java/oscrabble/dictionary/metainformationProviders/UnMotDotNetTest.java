package oscrabble.dictionary.metainformationProviders;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import oscrabble.dictionary.DictionaryException;
import oscrabble.dictionary.metainformationProviders.UnMotDotNet;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class UnMotDotNetTest
{


	@Test
	public void getDescription() throws DictionaryException
	{
		final UnMotDotNet provider = new UnMotDotNet();
		final ArrayList<String> words = new ArrayList<>();
		words.add("doxologies");
		//noinspection SpellCheckingInspection
		words.add("cabanée"); // this word is only known of 1mot.net as the base verb "cabaner"

		for (final String word : words)
		{
			final List<String> descriptions = provider.getDefinitions(word);
			Assertions.assertNotEquals(0, descriptions.size());
			for (final String description : descriptions)
			{
				final JLabel label = new JLabel(description);
				label.setSize(400, 0);
//				JOptionPane.showMessageDialog(null, label);
			}
		}

	}


}