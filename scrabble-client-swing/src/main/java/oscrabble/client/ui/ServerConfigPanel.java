package oscrabble.client.ui;

import oscrabble.controller.ScrabbleServerInterface;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class ServerConfigPanel extends JPanel {

	public ServerConfigPanel(final ScrabbleServerInterface server) {
		createAdditionalRefusedWords(server);
	}

	private void createAdditionalRefusedWords(final ScrabbleServerInterface server) {
		final AdditionalRefusedWordsPane refusedWordsPane = new AdditionalRefusedWordsPane(server);
		refusedWordsPane.setBorder(new TitledBorder("additional refused words"));
		add(refusedWordsPane);
		add(new JButton("dummy"));
		setPreferredSize(new Dimension(300, 200));
	}
}
