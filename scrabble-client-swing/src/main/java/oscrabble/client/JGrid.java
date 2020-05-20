package oscrabble.client;

import lombok.SneakyThrows;
import oscrabble.data.objects.Grid;
import oscrabble.data.objects.Square;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

/**
 * Darstellung der Spielfläche
 */
class JGrid extends JPanel
{
	final JSquare[][] jSquares;
	final JComponent background;
	private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
	private final HashMap<JSquare, MatteBorder> specialBorders = new HashMap<>();

	private List<JSquare> getAllSquares()
	{
		final ArrayList<JSquare> l = new ArrayList<>();
		for (final JSquare[] line : this.jSquares)
		{
			for (final JSquare jSquare : line)
			{
				if (jSquare != null)
				{
					l.add(jSquare);
				}
			}
		}
		return l;
	}

	/**
	 * Frame für die Anzeige der Definition von Wärtern
	 */
	private final DictionaryComponent dictionaryComponent;
	/**
	 * Set to let the new stones flash
	 */
	boolean hideNewStones;
	/** Turn number of the tiles to hide (for blinking) */
	UUID turnToHide;
	private Grid grid;
	/**
	 * Client mit dem diese Grid verknüpft ist
	 */
	private Playground playground;
	/**
	 * Last played action
	 */
	private UUID lastAction;

	/**
	 * Spielfeld des Scrabbles
	 */
	JGrid()
	{
		this.dictionaryComponent = new DictionaryComponent();

		this.setLayout(new BorderLayout());
		this.background = new JPanel(new BorderLayout());
		final int size = 15 * Playground.CELL_SIZE;  // TODO: use a constant
		this.jSquares = new JSquare[17][17];
		this.setPreferredSize(new Dimension(size, size));
		this.add(this.background);
	}

	/**
	 *
	 * @param grid grid description coming from the server
	 * @param playground playground this grid will belong to
	 */
	void setGrid(oscrabble.data.Grid grid, final Playground playground)
	{
		this.grid = new Grid(grid);
		final int numberOfRows = this.grid.getSize() + 2;

		final JPanel p1 = new JPanel();
		final GridBagLayout bagLayout = new GridBagLayout();
		p1.setLayout(bagLayout);
		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1;
		gbc.weighty = 1;

		// Draw each Cell
		for (int y = 0; y < numberOfRows; y++)
		{
			gbc.gridy = y;
			for (int x = 0; x < numberOfRows; x++)
			{
				gbc.gridx = x;
				final Square square = this.grid.get(x, y);
				if (square.isBorder)
				{
					p1.add(new BorderCell(square), gbc);
				}
				else
				{
					final JSquare cell = new JSquare(square);
					this.jSquares[x][y] = cell;
					cell.playground = playground;
					p1.add(cell, gbc);

					final Color cellColor;
					if (cell.square.letterBonus == 2)
					{
						cellColor = Color.decode("0x00BFFF");
					}
					else if (cell.square.letterBonus == 3)
					{
						cellColor = Color.blue;
					}
					else if (cell.square.wordBonus == 2)
					{
						cellColor = Color.decode("#F6CEF5").darker();
					}
					else if (cell.square.wordBonus == 3)
					{
						cellColor = Color.red;
					}
					else
					{
						cellColor = Playground.SCRABBLE_GREEN;
					}

					cell.setBackground(cellColor);
					cell.setOpaque(true);
					cell.setBorder(new LineBorder(Color.BLACK, 1));
				}
			}
		}

		this.background.removeAll();
		this.background.add(p1);
	}

	/**
	 * Start scheduler to let the last word blinks for some seconds.
	 */
	void scheduleLastWordBlink(final UUID turnToHide)
	{
		this.executor.schedule(
				(Callable<Void>) () -> {
					for (int i = 0; i < 3; i++)
					{
						this.turnToHide = null;
						this.playground.gridFrame.repaint();
						Thread.sleep(500);
						this.turnToHide = turnToHide;
						this.playground.gridFrame.repaint();
						Thread.sleep(500);
					}
					this.turnToHide = null;
					this.playground.gridFrame.repaint();
					return null;
				},
				0,
				TimeUnit.SECONDS);
	}

	void highlightWord(final oscrabble.controller.Action.PlayTiles action)
	{
		this.specialBorders.clear();
		this.getAllSquares().forEach( sq -> sq.preparedTile = null);

		final ArrayList<JSquare> squares = new ArrayList<>();
		boolean isHorizontal = action.getDirection() == Grid.Direction.HORIZONTAL;
		final int INSET = 4;
		final Color preparedMoveColor = Color.RED;

		int x = action.startSquare.x;
		int y = action.startSquare.y;
		for (int i=0; i < action.word.length(); i++)
		{
			if (x > 15 || y > 15)
			{
				// word runs outside of the grid.
				return;
			}

			squares.add(this.jSquares[x][y]);
			if (isHorizontal)
			{
				x++;
			}
			else
			{
				y++;
			}
		}

		// todo: merge with upper loop?
		for (int i = 0; i < squares.size(); i++)
		{
			final int top = (isHorizontal || i == 0) ? INSET : 0;
			final int left = (!isHorizontal || i == 0) ? INSET : 0;
			final int bottom = (isHorizontal || i == squares.size() - 1) ? INSET : 0;
			final int right = (!isHorizontal || i == squares.size() - 1) ? INSET : 0;

			final MatteBorder border = new MatteBorder(
					top, left, bottom, right, preparedMoveColor
			);

			final JSquare jSquare = squares.get(i);
			jSquare.preparedTile = action.word.charAt(i);

			this.specialBorders.put(
					jSquare,
					border
			);
		}

		repaint();
	}

	/**
	 * Holt und zeigt die Definitionen eines Wortes
	 *
	 * @param word Wort
	 */
	private void showDefinition(final String word)
	{
		Window dictionaryFrame = SwingUtilities.getWindowAncestor(this.dictionaryComponent);
		if (dictionaryFrame == null)
		{
			dictionaryFrame = new JFrame(Playground.MESSAGES.getString("description"));
			dictionaryFrame.add(this.dictionaryComponent);
			dictionaryFrame.setSize(600, 600);
		}


		this.dictionaryComponent.showDescription(word);
		dictionaryFrame.setVisible(true);
		dictionaryFrame.toFront();
	}

	void setPlayground(final Playground client)
	{
		if (this.playground != null)
		{
			throw new AssertionError("The client is already set");
		}
		this.playground = client;
	}

	/**
	 * Component für die Anzeige der Nummer und Buchstaben der Zeilen und Spalten des Grids.
	 */
	private static class BorderCell extends JComponent
	{
		private final Square square;

		BorderCell(final Square square)
		{
			this.square = square;
		}

		@Override
		protected void paintComponent(final Graphics g)
		{
			super.paintComponent(g);

			final Graphics2D g2 = (Graphics2D) g;
			final Insets insets = getInsets();

			// Wir erben direkt aus JComponent und müssen darum den Background selbst zeichnen todo check
			if (isOpaque() && getBackground() != null)
			{
				g2.setPaint(Color.lightGray);
				g2.fillRect(insets.right, insets.top, getWidth() - insets.left, getHeight() - insets.bottom);
			}

			// Draw the label
			g2.setColor(Color.BLACK);
			final Font font = g2.getFont().deriveFont(JTile.getCharacterSize(this)).deriveFont(Font.BOLD);
			g2.setFont(font);
			FontMetrics metrics = g2.getFontMetrics(font);

			final int x = this.square.getX();
			final int y = this.square.getY();
			if ((x > 0 && x < Grid.GRID_SIZE_PLUS_2 - 1) || (y > 0 && y < Grid.GRID_SIZE_PLUS_2 - 1))
			{
				final String label = x == 0 || x == Grid.GRID_SIZE_PLUS_2 - 1
						? Integer.toString(y)
						: Character.toString((char) ('@' + x));
				int tx = (getWidth() - metrics.stringWidth(label)) / 2;
				int ty = ((getHeight() - metrics.getHeight()) / 2) + metrics.getAscent();
				g2.drawString(label, tx, ty);
			}
		}
	}

	/**
	 * A cell of the scrabble field.
	 */
	class JSquare extends JPanel
	{
		final Square square;
		private Character preparedTile;
		/** Playground associated to this square */
		private Playground playground;

		JSquare(final Square square)
		{
			this.square = square;
			this.setLayout(new BorderLayout());
			this.setPreferredSize(JTile.CELL_DIMENSION);

			if (this.square.tile != null)
			{
				final JTile tile = new JTile(this.square.tile);
				add(tile);
				tile.grid = JGrid.this;
			}

			final AbstractAction showDefinitionAction = new AbstractAction("Show definitions") // todo: translate
			{
				@Override
				public void actionPerformed(final ActionEvent e)
				{
					JGrid.this.grid.getWords(square.getCoordinate()).forEach(
							word -> showDefinition(word)
					);
				}
			};
			final JPopupMenu popup = new JPopupMenu();
			popup.add(showDefinitionAction);
			setComponentPopupMenu(popup);

			this.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseClicked(final MouseEvent e)
				{
					if (e.getButton() == MouseEvent.BUTTON1)
					{
						JGrid.this.playground.setStartCell(JSquare.this);
					}
				}
			});
		}

		@SneakyThrows
		@Override
		protected void paintComponent(final Graphics g)
		{
			final Graphics2D g2 = (Graphics2D) g;
			if (this.preparedTile != null)
			{
				JTile.drawTile(g2, this, this.preparedTile, 1, false, Color.black);
			}
			else
			{
				super.paintComponent(g);
			}

			final MatteBorder specialBorder = JGrid.this.specialBorders.get(this);
			if (specialBorder != null)
			{
				specialBorder.paintBorder(
						this, g, 0, 0, getWidth(), getHeight()
				);
			}
		}

		@Override
		public void paint(final Graphics g)
		{
			super.paint(g);
			final Graphics2D g2 = (Graphics2D) g;
			// Markiert die Start Zelle des Wortes todo
			if (this.playground != null)
			{
				oscrabble.controller.Action.PlayTiles action;
				if (this.playground.action instanceof oscrabble.controller.Action.PlayTiles
						&& (action = ((oscrabble.controller.Action.PlayTiles) this.playground.action)).startSquare.getSquare().equals(this.square))
				{

					g2.setColor(Color.BLACK);
					final Polygon p = new Polygon();
					final int h = getHeight();
					final int POLYGONE_SIZE = h / 3;
					p.addPoint(-POLYGONE_SIZE / 2, 0);
					p.addPoint(0, POLYGONE_SIZE / 2);
					p.addPoint(POLYGONE_SIZE / 2, 0);

					final AffineTransform saved = ((Graphics2D) g).getTransform();
					switch (action.getDirection())
					{
						case VERTICAL:
							g2.translate(h / 2f, 6f);
							break;
						case HORIZONTAL:
							g2.rotate(-Math.PI / 2);
							g2.translate(-h / 2f, 6f);
							break;
						default:
							throw new IllegalStateException("Unexpected value: " + action.getDirection());
					}
					g2.fillPolygon(p);
					((Graphics2D) g).setTransform(saved);
				}
			}
		}
	}
}
