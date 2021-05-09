package oscrabble.client;

import oscrabble.client.utils.I18N;
import oscrabble.data.IDictionary;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Action displaying the definition of words in an own window.
 */
public class DisplayDefinitionAction extends AbstractAction {
	private static DictionaryComponent component = null;

	private final IDictionary dictionary;
	private final Supplier<Collection<String>> wordsSupplier;

	/**
	 * Component to position the window on.
	 */
	private Component relativeComponentPosition;

	public DisplayDefinitionAction(final IDictionary dictionary, final Supplier<Collection<String>> wordsSupplier) {
		super(I18N.get("show.definitions"));
		this.dictionary = dictionary;
		this.wordsSupplier = wordsSupplier;
	}

	public void setRelativeComponentPosition(final Component component) {
		this.relativeComponentPosition = component;
	}

	final Set<Runnable> beforeActionListeners = new LinkedHashSet<>();
	final Set<Runnable> afterActionListeners = new LinkedHashSet<>();

	@Override
	public void actionPerformed(final ActionEvent e) {
		this.beforeActionListeners.forEach(l -> l.run());
		try {
			if (component == null) {
				component = new DictionaryComponent(this.dictionary);
			}
			final Collection<String> words = this.wordsSupplier.get();
			if (words != null) {
				words.forEach(word -> showDefinition(word));
			}
		} finally {
			this.afterActionListeners.forEach(l -> l.run());
		}
	}

	private void showDefinition(final String word) {
		Window dictionaryFrame = SwingUtilities.getWindowAncestor(component);
		if (dictionaryFrame == null) {
			dictionaryFrame = new JFrame(I18N.get("description"));
			dictionaryFrame.add(component);
			dictionaryFrame.setSize(600, 200);
			if (this.relativeComponentPosition != null) {
				final Point pt = this.relativeComponentPosition.getLocation();
				pt.translate(this.relativeComponentPosition.getWidth() + 10, 10);
				dictionaryFrame.setLocation(pt);
			}
		}


		component.showDescription(word);
		dictionaryFrame.setVisible(true);
		dictionaryFrame.toFront();
	}

}

