package oscrabble.client.ui;

import javax.swing.*;
import java.awt.*;

/**
 * This text pane treat the tab key as a change between the widget and not as the
 * insertion of a tab character in the text itself.
 */
public class JTextPaneWithoutTab extends JTextPane {

	public JTextPaneWithoutTab() {
		super();

		setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
	}
}
