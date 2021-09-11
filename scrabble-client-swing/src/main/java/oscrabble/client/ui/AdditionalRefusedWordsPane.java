package oscrabble.client.ui;

import oscrabble.ScrabbleException;
import oscrabble.client.Application;
import oscrabble.controller.ScrabbleServerInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.UUID;

class AdditionalRefusedWordsPane extends JTextPaneWithoutTab {

	private final ScrabbleServerInterface server;
	private final UUID gameId;

	public AdditionalRefusedWordsPane(final ScrabbleServerInterface server) {
		this.server = server;
		this.gameId = Application.getGameId();

		setPreferredSize(new Dimension(200, 200));
		addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(final FocusEvent e) {
				final HashSet<String> words = new HashSet<>();
				for (final String word : getText().split(" |\\r?\\n")) { //NON-NLS
					words.add(word.trim().toLowerCase());
				}
				words.remove("");
				try {
					server.setAdditionalRefusedWords(AdditionalRefusedWordsPane.this.gameId, words);
				} catch (ScrabbleException ex) {
					JOptionPane.showMessageDialog(AdditionalRefusedWordsPane.this, ex.toString());
				}
				updateListFromServer();
			}
		});

		updateListFromServer();
	}

	private void updateListFromServer() {
		final TreeSet<String> refusedWords = new TreeSet<>();
		this.server.getAdditionalRefusedWords(this.gameId).forEach(w -> refusedWords.add(w.toLowerCase()));
		final StringBuilder sb = new StringBuilder();
		refusedWords.forEach(w -> sb.append(w).append("\n"));
		if (sb.length() > 0) {
			sb.setLength(sb.length() - 1);
		}
		setText(sb.toString());
	}

}
