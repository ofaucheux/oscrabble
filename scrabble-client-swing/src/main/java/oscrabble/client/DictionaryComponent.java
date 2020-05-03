package oscrabble.client;

import oscrabble.client.dictionary.DictionaryException;
import oscrabble.client.dictionary.DictionaryService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Component mit Reitern. Jeder Reiter zeigt die Definition eines Wortes.
 */
public class DictionaryComponent extends JTabbedPane
{

	/**
	 * Liste der gefundenen Definition
	 */
	private final Set<String> found = new HashSet<>();

	private DictionaryService dico;

	/**
	 * Erstellt ein {@link DictionaryComponent}-
	 */
	public DictionaryComponent()
	{
		// add a word
		insertTab("+", null,null, "Search a word...", 0);
		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(final MouseEvent e)
			{
				final Point point = e.getPoint();
				final int index = indexAtLocation(point.x, point.y);
				if (index == 0)
				{
					final String wordToSearch = JOptionPane.showInputDialog(
							DictionaryComponent.this,
							"Word to search",
							"Search a word...",
							JOptionPane.PLAIN_MESSAGE);
					if (wordToSearch != null && !wordToSearch.trim().isEmpty())
					{
						showDescription(wordToSearch);
					}
				}
			}
		});
	}

	/**
	 * (Gfs. holt) und zeigt die Definition eines Wortes an.
	 * @param word das Wort zum Anzeigen.
	 */
	public void showDescription(final String word)
	{
		// tests if definition already displayed
		final int tabCount = this.getTabCount();
		for (int i = 0; i < tabCount; i++)
		{
			if (getTitleAt(i).equals(word))
			{
				if (this.found.contains(word))
				{
					setSelectedIndex(i);
					return;
				}
				else
				{
					remove(i);
				}
			}
		}

		Iterable<String> descriptions;
		if (this.dico != null)
		{
			try
			{
				descriptions = this.dico.getDefinitions(word);
				this.found.add(word);
			}
			catch (DictionaryException e)
			{
				descriptions = null;
			}
		}
		else
		{
			descriptions = Collections.singleton("No word meta info provider");
		}

		final JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		if (descriptions == null)
		{
			panel.add(new JLabel(word + ": no definition found"));
		}
		else
		{
			descriptions.forEach(description -> panel.add(new JLabel(String.valueOf(description))));
		}

		final JScrollPane sp = new JScrollPane(panel);
		addTab(word, sp);
		setSelectedComponent(sp);
	}

}
