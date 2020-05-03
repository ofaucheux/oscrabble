package oscrabble.client;

import oscrabble.data.objects.Grid;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Darstellung eines Spielstein.
 */
class JTile extends JComponent
{

	final static int CELL_SIZE = 40;
	final static Dimension CELL_DIMENSION = new Dimension(CELL_SIZE, CELL_SIZE);
	private final boolean isJoker;
	private final char letter;
	private final int value;

	JTile(final char letter, final int value, final boolean isJoker)
	{
		this.letter = letter;
		this.value = value;
		this.isJoker = isJoker;
		setPreferredSize(CELL_DIMENSION);
		setTransferHandler(new TransferHandler("name"));
		addMouseListener(new DragMouseAdapter());
	}

	public JTile(final Grid.Square square)
	{
		this(square.c, square.letterBonus, square.isJoker());
	}

	public static void drawStone(final Graphics2D g2, final Container container, final JTile tile, final Color black)
	{
		drawStone(g2, container, tile.letter, tile.value, tile.isJoker, black);
	}

	@Override
	protected void paintComponent(final Graphics g)
	{
		super.paintComponent(g);
		drawStone((Graphics2D) g, this, this.letter, this.value, this.isJoker, Color.black);
	}

	private static final Color STONE_BACKGROUND_COLOR = Color.decode("0xF3E5AB");
	private static final int ARC_WIDTH = 14;

	static void drawStone(final Graphics2D g2,
						  final Container component,
						  final char letter,
						  final int value,
						  final boolean isJoker,
						  final Color foregroundColor)
	{

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

		if (letter != ' ')
		{
			final float characterSize = getCharacterSize(component);

			// Draw the letter
			g2.setColor(isJoker ? Color.GRAY : foregroundColor);
			final Font font = g2.getFont().deriveFont(characterSize).deriveFont(Font.BOLD);
			g2.setFont(font);
			FontMetrics metrics = g2.getFontMetrics(font);
			final String str = Character.toString(letter);
			int tx = (component.getWidth() - metrics.stringWidth(str) / 2);
			int ty = ((component.getHeight() - metrics.getHeight()) / 2) + metrics.getAscent();
			g2.drawString(str, tx, ty);


			// Draw the point-value
			if (value != 0)
			{
				g2.setFont(font.deriveFont(characterSize * 10 / 18));
				metrics = g2.getFontMetrics(font);
				final String points = Integer.toString(value);
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

	private static class DragMouseAdapter extends MouseAdapter
	{

		public void mousePressed(MouseEvent e) {

			JComponent c = (JComponent) e.getSource();
			TransferHandler handler = c.getTransferHandler();
			handler.exportAsDrag(c, e, TransferHandler.COPY);
		}
	}
}