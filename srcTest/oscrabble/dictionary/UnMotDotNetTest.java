package oscrabble.dictionary;

import org.junit.jupiter.api.Test;

import javax.swing.*;

import static org.junit.jupiter.api.Assertions.*;

class UnMotDotNetTest
{


	@Test
	void getDescription()
	{
		final UnMotDotNet dico = new UnMotDotNet();
		final String description = dico.getDescription("doxologies");
		JOptionPane.showMessageDialog(null, new JLabel(description));

	}
}