package oscrabble.client;

import oscrabble.controller.MicroServiceDictionary;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.function.Supplier;

/**
 * Action displaying the definition of words in an own window.
 */
public class DisplayDefinitionAction extends AbstractAction
{
	private static DictionaryComponent component = null;

	private final MicroServiceDictionary dictionary;
	private final Supplier<Collection<String>> wordsSupplier;

	public DisplayDefinitionAction(final MicroServiceDictionary dictionary, final Supplier<Collection<String>> wordsSupplier)
	{
		super("Show definitions");  // todo: i18n
		this.dictionary = dictionary;
		this.wordsSupplier = wordsSupplier;
	}

	@Override
	public void actionPerformed(final ActionEvent e)
	{
		if (component == null)
		{
			component = new DictionaryComponent(this.dictionary);
		}
		this.wordsSupplier.get().forEach(word -> showDefinition(word));
	}

	private void showDefinition(final String word)
	{
		Window dictionaryFrame = SwingUtilities.getWindowAncestor(component);
		if (dictionaryFrame == null)
		{
			dictionaryFrame = new JFrame(Playground.MESSAGES.getString("description"));
			dictionaryFrame.add(component);
			dictionaryFrame.setSize(600, 600);
		}


		component.showDescription(word);
		dictionaryFrame.setVisible(true);
		dictionaryFrame.toFront();
	}

}

