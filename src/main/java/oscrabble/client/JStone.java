package oscrabble.client;

import oscrabble.Stone;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Darstellung eines Spielstein.
 */
class JStone extends JComponent
{

	final static int CELL_SIZE = 40;
	final static Dimension CELL_DIMENSION = new Dimension(CELL_SIZE, CELL_SIZE);

	private final Stone stone;

	JStone(final Stone stone)
	{
		this.stone = stone;
		setPreferredSize(CELL_DIMENSION);
		setTransferHandler(new TransferHandler("name"));
		addMouseListener(new DragMouseAdapter());
	}


	@Override
	protected void paintComponent(final Graphics g)
	{
		super.paintComponent(g);
		drawStone((Graphics2D) g, this, this.stone, Color.black);
	}

	private static final Color STONE_BACKGROUND_COLOR = Color.decode("0xF3E5AB");
	private static final int ARC_WIDTH = 14;

	static void drawStone(final Graphics2D g2,
						  final Container component,
						  final Stone stone,
						  final Color foregroundColor)
	{
		if (stone == null)
		{
			return;
		}

		g2.setPaint(STONE_BACKGROUND_COLOR);
		final Insets insets = component.getInsets();
		//noinspection SuspiciousNameCombination
		g2.fillRoundRect(
				insets.right,
				insets.top,
				component.getWidth() - insets.left - insets.right,
				component.getHeight() - insets.bottom - insets.top,
				ARC_WIDTH,
				ARC_WIDTH);

		if (stone.hasCharacterSet())
		{
			final float characterSize = getCharacterSize(component);

			// Draw the letter
			g2.setColor(stone.isJoker() ? Color.GRAY : foregroundColor);
			final Font font = g2.getFont().deriveFont(characterSize).deriveFont(Font.BOLD);
			g2.setFont(font);
			final String letter = Character.toString(stone.getChar());
			FontMetrics metrics = g2.getFontMetrics(font);
			int tx = (component.getWidth() - metrics.stringWidth(letter)) / 2;
			int ty = ((component.getHeight() - metrics.getHeight()) / 2) + metrics.getAscent();
			g2.drawString(letter, tx, ty);


			// Draw the point-value
			if (stone.getPoints() != 0)
			{
				g2.setFont(font.deriveFont(characterSize * 10 / 18));
				metrics = g2.getFontMetrics(font);
				final String points = Integer.toString(stone.getPoints());
				int px = (component.getWidth() * 4 / 5) - (metrics.stringWidth(points) / 2);
				int py = (component.getHeight() * 3 / 4) - (metrics.getHeight() / 2) + metrics.getAscent() - 1;
				g2.drawString(points, px, py);
			}
		}
	}

	static float getCharacterSize(final Container cell)
	{
		return cell.getWidth() * 18 / 32f;
	}

	private class DragMouseAdapter extends MouseAdapter
	{

		public void mousePressed(MouseEvent e) {

			JComponent c = (JComponent) e.getSource();
			TransferHandler handler = c.getTransferHandler();
			handler.exportAsDrag(c, e, TransferHandler.COPY);
		}
	}
}