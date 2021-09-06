package oscrabble.client.ui;

import org.jdesktop.swingx.JXCollapsiblePane;
import oscrabble.client.Application;
import oscrabble.controller.ScrabbleServerInterface;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.UUID;

public class ServerConfigPanel extends JPanel {

	private final JTextPane refusedWords;

	public ServerConfigPanel(final ScrabbleServerInterface server) {
		this.refusedWords = new JTextPane();
		this.refusedWords.setPreferredSize(new Dimension(200, 200));
		final UUID gameId = Application.getGameId();
		this.refusedWords.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(final FocusEvent e) {
				final HashSet<String> words = new HashSet<>();
				for (final String word : ServerConfigPanel.this.refusedWords.getText().split("\\r?\\n")) { //NON-NLS
					words.add(word.trim().toLowerCase());
				}
				words.remove("");
				server.setRefusedWords(gameId, words);
				updateRefusedWordPanel(server, gameId);
			}
		});
		server.getRefusedWords(gameId);
		final JXCollapsiblePane refusedWordsPane = new JXCollapsiblePane();
		refusedWordsPane.setBorder(new TitledBorder("refused words"));
		refusedWordsPane.add(this.refusedWords);
		add(refusedWordsPane);
		add(new JButton("dummy"));
		setPreferredSize(new Dimension(300, 200));
	}

	private void updateRefusedWordPanel(final ScrabbleServerInterface server, final UUID gameId) {
		final TreeSet<String> refusedWords = new TreeSet<>();
		server.getRefusedWords(gameId).forEach(w -> refusedWords.add(w.toLowerCase()));
		final StringBuilder sb = new StringBuilder();
		refusedWords.forEach(w -> sb.append(w).append("\n"));
		sb.setLength(sb.length() - 1);
		this.refusedWords.setText(sb.toString());
	}
}
