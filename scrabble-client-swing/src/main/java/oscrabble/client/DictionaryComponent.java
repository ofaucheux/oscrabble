package oscrabble.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.client.utils.I18N;
import oscrabble.data.DictionaryEntry;import oscrabble.data.IDictionary;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;

/**
 * Component mit Reitern. Jeder Reiter zeigt die Definition eines Wortes.
 */
public class DictionaryComponent extends JTabbedPane {

	protected static final Logger LOGGER = LoggerFactory.getLogger(DictionaryComponent.class);

	/**
	 * Liste der gefundenen Definition
	 */
	private final Set<String> found = new HashSet<>();

	private final IDictionary dictionary;

	/**
	 * Erstellt ein {@link DictionaryComponent}-
	 *
	 * @param dictionary
	 */
	public DictionaryComponent(final IDictionary dictionary) {
		this.dictionary = dictionary;

		// add a word
		insertTab("+", null, null, I18N.get("search.a.word"), 0);
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent e) {
				final Point point = e.getPoint();
				final int index = indexAtLocation(point.x, point.y);
				if (index == 0) {
					final String wordToSearch = JOptionPane.showInputDialog(
							DictionaryComponent.this,
							I18N.get("word.to.search"),
							I18N.get("search.a.word"),
							JOptionPane.PLAIN_MESSAGE);
					if (wordToSearch != null && !wordToSearch.trim().isEmpty()) {
						showDescription(wordToSearch);
					}
				}
			}
		});
	}

	/**
	 * (Gfs. holt) und zeigt die Definition eines Wortes an.
	 *
	 * @param word das Wort zum Anzeigen.
	 */
	public void showDescription(final String word) {
		if (this.dictionary == null) {
			return;
		}

		// tests if definition already displayed
		final int tabCount = this.getTabCount();
		for (int i = 0; i < tabCount; i++) {
			if (getTitleAt(i).equals(word)) {
				if (this.found.contains(word)) {
					setSelectedIndex(i);
					return;
				} else {
					remove(i);
				}
			}
		}

		final JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		try {
			final DictionaryEntry entry = this.dictionary.getEntry(word);
			this.found.add(word);
			if (entry.definitions.isEmpty()) {
				panel.add(new JLabel(MessageFormat.format(I18N.get("0.no.definition.found"), word)));
			} else {
				entry.definitions.forEach(definition -> panel.add(new JLabel(String.valueOf(definition))));
			}
		} catch (Throwable e) {
			LOGGER.error("Cannot load word", e); //NON-NLS
			panel.add(new JLabel(MessageFormat.format(I18N.get("error.0"), e)));
		}



		final JScrollPane sp = new JScrollPane(panel);
		addTab(word, sp);
		setSelectedComponent(sp);
	}
}
