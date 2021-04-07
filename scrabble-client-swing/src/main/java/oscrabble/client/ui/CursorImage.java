package oscrabble.client.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.LineMetrics;

/**
 * An image following the cursor and displaying a text.
 */
public class CursorImage extends JComponent {

	// Information about the implementation: the size of the cursor canndt be changed,
	// at least not on windows. We therefore have to draw the text by ourselves and
	// cannot use "setCursor".
	//
	// This image will be drawn on the one upper layer of the layered pane of the window.
	//
	// AWT events are only dispatched on one component. We cannot register a mouse listener
	// for the whole window, as it will have no effect when the cursor is on one of their
	// specific components.
	// As solution we register a global AWT listener and look if the cursor is on the window.

	private final Color color;
	private final Font font = Font.decode("Arial-PLAIN-10");
	private final Dimension cursorSize;
	private String text;
	private AWTEventListener mouseMoveListener;

	/**
	 *
	 * @param text
	 * @param color
	 */
	public CursorImage(final String text, final Color color) {
		this.text = text;
		this.color = color;

		this.cursorSize = Toolkit.getDefaultToolkit().getBestCursorSize(16, 16);
		setSize(this.cursorSize.width + 100, 16);
	}

	/**
	 *
	 * @param window
	 */
	public void setOnWindow(JFrame window) {
		// remove
		if (this.getParent() != null) {
			this.getParent().remove(this);
			Toolkit.getDefaultToolkit().removeAWTEventListener(this.mouseMoveListener);
		}

		final Container layeredPane = window.getRootPane().getLayeredPane();
		layeredPane.add(this, JLayeredPane.DRAG_LAYER);
		this.mouseMoveListener = event -> {
			if (SwingUtilities.isDescendingFrom(((Component) event.getSource()), window)) {
				relocate();
			}
		};

		Toolkit.getDefaultToolkit().addAWTEventListener(this.mouseMoveListener, AWTEvent.MOUSE_MOTION_EVENT_MASK);
		relocate();
	}

	/**
	 *
	 * @param text
	 */
	public void setText(String text) {
		this.text = text;
	}

	@Override
	protected void paintComponent(Graphics g) {
		if (this.text == null) {
			return;
		}
		g = g.create();
		g.setColor(this.color);
		g.setFont(this.font);
		g.drawString(this.text, this.cursorSize.width / 2, this.getHeight());
	}

	/**
	 * Set the image at the current mouse position.
	 */
	private void relocate() {
		final Container parent = getParent();
		if (parent != null) {
			final Point position = parent.getMousePosition();
			if (position != null) {
				this.setLocation(position);
				this.repaint();
			}
		}
	}
}
