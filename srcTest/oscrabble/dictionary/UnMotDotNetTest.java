package oscrabble.dictionary;

import org.junit.jupiter.api.Test;

import javax.swing.*;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class UnMotDotNetTest
{


	@Test
	void getDescription()
	{
		final UnMotDotNet dico = new UnMotDotNet();
		final ArrayList<String> words = new ArrayList<>();
//		words.add("doxologies");
		words.add("cabanée"); // this word is only known of 1mot.net as the base verb "cabaner"

		for (final String word : words)
		{
			final String description = dico.getDescription(word);
			JOptionPane.showMessageDialog(null, new JLabel(description));
		}

	}


}