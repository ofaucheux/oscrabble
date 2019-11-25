package oscrabble.client;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.log4j.Logger;
import oscrabble.*;
import oscrabble.configuration.ConfigurationPanel;
import oscrabble.dictionary.Dictionary;
import oscrabble.dictionary.DictionaryException;
import oscrabble.player.AbstractPlayer;
import oscrabble.player.BruteForceMethod;
import oscrabble.server.IAction;
import oscrabble.server.IPlayerInfo;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.text.Normalizer;
import java.text.ParseException;
import java.util.List;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SwingClient extends AbstractPlayer
{
	private final static int CELL_SIZE = 40;
	public static final Logger LOGGER = Logger.getLogger(SwingClient.class);
	private static final Pattern PATTERN_EXCHANGE_COMMAND = Pattern.compile("-\\s*(.*)");
	private static final Color SCRABBLE_GREEN = Color.green.darker().darker();

	private JGrid jGrid;
	private JTextField commandPrompt;
	private JRack jRack;
	private JScoreboard jScoreboard;

	private TelnetFrame telnetFrame;

	/** Panel for the display of possible moves and corresponding buttons */
	private JPanel possibleMovePanel;

	/** Button to display / hide the possible moves */
	private JButton showPossibilitiesButton;

	/** Thread executor */
	private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(5);

	/** Future to let the last played stone flash */
	private ScheduledFuture<Object> flashFuture;

	/** Listing of the history of the game */
	private JList<Game.HistoryEntry> historyList;

	public SwingClient(final String name)
	{
		super(name);
	}

	/**
	 * create UI and display it
	 */
	private void display()
	{
		this.jGrid = new JGrid(getGrid(), this.game.getDictionary());
		this.jGrid.setClient(this);
		this.jRack = new JRack();
		this.jScoreboard = new JScoreboard();
		this.commandPrompt = new JTextField();
		final CommandPromptAction promptListener = new CommandPromptAction();
		this.commandPrompt.addActionListener(promptListener);
		this.commandPrompt.setFont(this.commandPrompt.getFont().deriveFont(20f));
		final AbstractDocument document = (AbstractDocument) this.commandPrompt.getDocument();
		document.addDocumentListener(promptListener);
		document.setDocumentFilter(UPPER_CASE_DOCUMENT_FILTER);
		this.telnetFrame = new TelnetFrame("Help");

		final JFrame gridFrame = new JFrame();
		gridFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		gridFrame.setLayout(new BorderLayout());
		gridFrame.add(this.jGrid);

		final JPanel eastPanel = new JPanel(new BorderLayout());
		final JPanel panel1 = new JPanel();
		panel1.setLayout(new BoxLayout(panel1, BoxLayout.PAGE_AXIS));
		panel1.add(this.jScoreboard);
		panel1.add(Box.createVerticalGlue());

		final JPanel historyPanel = new JPanel();
		historyPanel.setBorder(new TitledBorder("Moves"));
		this.historyList = new JList<>();
		this.historyList.setCellRenderer(new DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus)
			{
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				setText(((Game.HistoryEntry) value).print());
				return this;
			}
		});
		historyPanel.setSize(new Dimension(200, 300));
		historyPanel.add(new JScrollPane(this.historyList));
		panel1.add(historyPanel);

		this.possibleMovePanel = new JPanel();
		this.possibleMovePanel.setBorder(new TitledBorder("Possible moves"));
		this.possibleMovePanel.setSize(new Dimension(200, 300));
		this.possibleMovePanel.setLayout(new BorderLayout());
		final BruteForceMethod bruteForceMethod = new BruteForceMethod(this.game.getDictionary());
		this.showPossibilitiesButton = new JButton(new PossibleMoveDisplayer(bruteForceMethod));
		this.showPossibilitiesButton.setFocusable(false);
		resetPossibleMovesPanel();

		panel1.add(this.possibleMovePanel);
		panel1.add(Box.createVerticalGlue());

		final ConfigurationPanel configPanel = new ConfigurationPanel(this.game.getConfiguration());
		panel1.add(configPanel);
		configPanel.setBorder(new TitledBorder("Server configuration"));
		eastPanel.add(panel1, BorderLayout.CENTER);
		gridFrame.add(eastPanel, BorderLayout.LINE_END);

		gridFrame.add(this.commandPrompt, BorderLayout.SOUTH);
		gridFrame.pack();
		gridFrame.setResizable(false);
		gridFrame.setVisible(true);

		final Window rackFrame = new JDialog(gridFrame);
		rackFrame.setLayout(new BorderLayout());
		rackFrame.add(this.jRack);

		final JButton exchangeButton = new JButton((new ExchangeTilesAction()));
		exchangeButton.setToolTipText(exchangeButton.getText());
		exchangeButton.setHideActionText(true);
		final Dimension dim = new Dimension(30, 20);
		exchangeButton.setMaximumSize(dim);
		exchangeButton.setPreferredSize(dim);
		exchangeButton.setIcon(exchangeButton.getIcon());

		rackFrame.add(exchangeButton, BorderLayout.AFTER_LINE_ENDS);
		rackFrame.pack();
		rackFrame.setVisible(true);
		rackFrame.setLocation(
					gridFrame.getX() + gridFrame.getWidth(),
					gridFrame.getY() + gridFrame.getHeight() / 2
			);
		rackFrame.setFocusableWindowState(false);
		rackFrame.setFocusable(false);

		this.telnetFrame.frame.setVisible(false);
		this.telnetFrame.frame.setSize(new Dimension(300, 300));
		this.telnetFrame.frame.setLocationRelativeTo(rackFrame);
		this.telnetFrame.frame.setLocation(rackFrame.getX(), rackFrame.getY() + rackFrame.getHeight());

		SwingUtilities.invokeLater(() -> {
			gridFrame.requestFocus();
			this.commandPrompt.requestFocusInWindow();
			this.commandPrompt.grabFocus();
		});

		KeyboardFocusManager.setCurrentKeyboardFocusManager(new DefaultFocusManager() {
			@Override
			public boolean dispatchKeyEvent(final KeyEvent e)
			{
				SwingClient.this.commandPrompt.requestFocus();
				e.setSource(SwingClient.this.commandPrompt);
				return super.dispatchKeyEvent(e);
			}
		});

	}

	private void resetPossibleMovesPanel()
	{
		this.possibleMovePanel.removeAll();
		this.possibleMovePanel.invalidate();
		this.possibleMovePanel.repaint();
		this.showPossibilitiesButton.setText(PossibleMoveDisplayer.LABEL_DISPLAY);
		this.possibleMovePanel.add(this.showPossibilitiesButton, BorderLayout.SOUTH);
	}

	void setCommandPrompt(final String text)
	{
		this.commandPrompt.setText(text);
	}

	@Override
	public void onPlayRequired(final AbstractPlayer currentPlayer)
	{
		for (final Map.Entry<IPlayerInfo, JScoreboard.ScorePanelLine> entry : this.jScoreboard.scoreLabels.entrySet())
		{
			final IPlayerInfo playerInfo = entry.getKey();
			final JScoreboard.ScorePanelLine line = entry.getValue();
			line.currentPlaying.setVisible(playerInfo.getName().equals(currentPlayer.getName()));
		}

		final Cursor cursor;
		if (currentPlayer == this)
		{
			this.jRack.update();
			cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
		}
		else
		{
			cursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
		}

		for (final Frame frame : Frame.getFrames())
		{
			frame.setCursor(cursor);
		}
	}

	@Override
	public void onDictionaryChange()
	{
		// nichts
	}

	@Override
	public void onDispatchMessage(final String msg)
	{
		showMessage(msg);
	}

	@Override
	public void afterPlay(final int moveNr, final IPlayerInfo info, final IAction action, final int score)
	{
		this.jGrid.lastAction = action;
		if (this.flashFuture != null)
		{
			this.flashFuture.cancel(true);
		}

		this.flashFuture = this.executor.schedule(
				() -> {
					for (int i = 0; i < 3; i++)
					{
						Thread.sleep(5 * 100);
						SwingClient.this.jGrid.hideNewStones = !SwingClient.this.jGrid.hideNewStones;
						SwingClient.this.jGrid.repaint();
					}
					SwingClient.this.jGrid.hideNewStones = false;
					SwingClient.this.jGrid.repaint();
					this.flashFuture = null;
					return null;
				},
				0,
				TimeUnit.SECONDS);

		this.jGrid.repaint();
		this.jScoreboard.refreshDisplay(info);

		this.historyList.setListData(IterableUtils.toList(this.game.getHistory()).toArray(new Game.HistoryEntry[0]));
		if (info.getName().equals(this.getName()))
		{
			this.jRack.update();
		}
	}

	@Override
	public void beforeGameStart()
	{
		this.display();
		this.jScoreboard.prepareBoard();
		this.jRack.update();
	}

	@Override
	public boolean isObserver()
	{
		return false;
	}

	/**
	 * Panel for the display of the actual score.
	 */
	private class JScoreboard extends JPanel
	{

		private final HashMap<IPlayerInfo, ScorePanelLine> scoreLabels = new HashMap<>();

		JScoreboard()
		{
			setPreferredSize(new Dimension(200, 0));
			setLayout(new GridBagLayout());
			setBorder(new TitledBorder("Score"));
		}

		void refreshDisplay(final IPlayerInfo playerInfo)
		{
			this.scoreLabels.get(playerInfo).score.setText(playerInfo.getScore() + " pts");
		}

		void prepareBoard()
		{
			final double SMALL_WEIGHT = 0.1;
			final double BIG_WEIGHT = 10;

			final Dimension buttonDim = new Dimension(20, 20);
			final List<IPlayerInfo> players = SwingClient.this.game.getPlayers();
			final GridBagConstraints c = new GridBagConstraints();
			for (final IPlayerInfo player : players)
			{
				final ScorePanelLine line = new ScorePanelLine();
				this.scoreLabels.put(player, line);

				c.insets = new Insets(0, 0, 0, 0);
				c.gridy++;
				c.gridx = 0;
				c.weightx = SMALL_WEIGHT;
				line.currentPlaying = new JLabel("►");
				line.currentPlaying.setPreferredSize(buttonDim);
				line.currentPlaying.setVisible(false);
				add(line.currentPlaying, c);

				c.gridx++;
				c.weightx = BIG_WEIGHT;
				c.anchor = GridBagConstraints.LINE_START;
				final String name = player.getName();
				add(new JLabel(name), c);
				c.weightx = SMALL_WEIGHT;

				c.gridx++;
				c.anchor = GridBagConstraints.LINE_END;
				line.score = new JLabel();
				add(line.score, c);

				c.gridx++;
				line.parameterButton = new JButton();
				line.parameterButton.setPreferredSize(buttonDim);
				line.parameterButton.setFocusable(false);
				line.parameterButton.setAction(new AbstractAction("...")
				{
					@Override
					public void actionPerformed(final ActionEvent e)
					{
						final SwingWorker<Void, Void> worker = new SwingWorker<>()
						{
							@Override
							protected Void doInBackground()
							{
								SwingClient.this.game.editParameters(SwingClient.this.playerKey, player);
								return null;
							}
						};
						worker.execute();
					}
				});
				line.parameterButton.setVisible(player.hasEditableParameters());
				add(line.parameterButton, c);

			}

			c.gridy++;
			c.gridx = 0;
			c.weighty = 5.0f;
			add(new JPanel(), c);

			setPreferredSize(new Dimension(200, 50 * players.size()));
			getParent().validate();
		}

		private class ScorePanelLine
		{
			private JLabel score;
			private JLabel currentPlaying;
			private JButton parameterButton;
		}
	}

	private class JRack extends JPanel
	{
		static final int RACK_SIZE = 7;
		final RackCell[] cells = new RackCell[7];

		private JRack()
		{
			this.setLayout(new GridLayout(1,7));
			for (int i = 0; i < RACK_SIZE; i++)
			{
				this.cells[i] = new RackCell();
				add(this.cells[i]);
			}
		}

		void update()
		{
			try
			{
				final ArrayList<Stone> stones = new ArrayList<>(
						SwingClient.this.game.getRack(SwingClient.this, SwingClient.this.playerKey));

				for (int i = 0; i < RACK_SIZE; i++)
				{
					this.cells[i].setStone(
							i >= stones.size() ? null : stones.get(i)
					);
				}
				this.repaint();
			}
			catch (ScrabbleException e)
			{
				showMessage(e.getMessage());
			}
		}
	}

	private void showMessage(final Object message)
	{
		final KeyboardFocusManager previous = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		KeyboardFocusManager.setCurrentKeyboardFocusManager(new DefaultFocusManager());
		try
		{
			JOptionPane.showMessageDialog(null, message);
		}
		finally
		{
			KeyboardFocusManager.setCurrentKeyboardFocusManager(previous);
		}

	}

	/**
	 * Darstellung der Spielfläche
	 */
	static class JGrid extends JPanel
	{
		private final HashMap<Grid.Square, MatteBorder> specialBorders = new HashMap<>();

		private final Grid grid;
		private final Dictionary dictionary;
		private final Map<Grid.Square, Stone> preparedMoveStones;

		final JComponent background;

		/** Client mit dem diese Grid verknüpft ist */
		private SwingClient client;

		/** Vorbereiteter Spielzug */
		private Move preparedMove;

		/** Set to let the new stones flash	*/
		private boolean hideNewStones;

		/** Last played action */
		private IAction lastAction;

		/** Spielfeld des Scrabbles */
		JGrid(final Grid grid, final Dictionary dictionary)
		{
			this.grid = grid;
			final int numberOfRows = grid.getSize() + 2;
			this.dictionary = dictionary;

			this.setLayout(new BorderLayout());
			this.background = new JPanel();
			this.background.setLayout(new GridLayout(numberOfRows, numberOfRows));

			// Draw each Cell
			final int borderColumn = numberOfRows - 1;
			for (int y = 0; y < numberOfRows; y++)
			{
				for (int x = 0; x < numberOfRows; x++)
				{
					if (x == 0 || x == borderColumn)
					{
						this.background.add(new BorderCell(
								y == 0 || y == borderColumn ? "" : Integer.toString(y)));
					}
					else if (y == 0 || y == borderColumn)
					{
						this.background.add(new BorderCell(Character.toString((char) ((int) 'A' + x - 1))));
					}
					else
					{
						final StoneCell cell = new StoneCell(x, y);
						this.background.add(cell);

						final Color cellColor;
						switch (cell.square.getBonus())
						{
							case NONE:
								cellColor = SCRABBLE_GREEN;
								break;
							case BORDER:
								cellColor = Color.black;
								break;
							case LIGHT_BLUE:
								cellColor = Color.decode("0x00BFFF");
								break;
							case DARK_BLUE:
								cellColor = Color.blue;
								break;
							case RED:
								cellColor = Color.red;
								break;
							case ROSE:
								cellColor = Color.decode("#F6CEF5").darker();
								break;
							default:
								throw new AssertionError();
						}

						cell.setBackground(cellColor);
						cell.setOpaque(true);
						cell.setBorder(new LineBorder(Color.BLACK, 1));
					}
				}
			}
			final int size = numberOfRows * CELL_SIZE;
			this.setPreferredSize(new Dimension(size, size));
			this.add(this.background);
			this.preparedMoveStones = new LinkedHashMap<>();
		}

		/**
		 * Zeigt den vorbereiteten Spielzug auf dem Grid
		 * @param move der Zug zu zeigen. {@code null} für gar keinen Zug.
		 */
		void setPreparedMove(final Move move)
		{
			this.preparedMove = move;
			this.preparedMoveStones.clear();
			// Calculate the border for prepared word
			this.specialBorders.clear();
			if (move != null)
			{
				this.preparedMoveStones.putAll(move.getStones(this.grid, this.dictionary));
				final int INSET = 4;
				final Color preparedMoveColor = Color.RED;
				final boolean isHorizontal = move.getDirection() == Move.Direction.HORIZONTAL;
				final ArrayList<Grid.Square> squares = new ArrayList<>(this.preparedMoveStones.keySet());
				for (int i = 0; i < squares.size(); i++)
				{
					final int top = (isHorizontal || i == 0) ? INSET : 0;
					final int left = (!isHorizontal || i == 0) ? INSET : 0;
					final int bottom = (isHorizontal || i == squares.size() - 1) ? INSET : 0;
					final int right = (!isHorizontal || i == squares.size() - 1) ? INSET : 0;

					final MatteBorder border = new MatteBorder(
							top, left, bottom, right, preparedMoveColor
					);

					this.specialBorders.put(
							squares.get(i),
							border
					);

				}
			}

			repaint();
		}


		private JFrame descriptionFrame;
		private JTabbedPane descriptionTabPanel;

		/**
		 * Holt und zeigt die Definitionen eines Wortes
		 * @param word Wort
		 */
		private void showDefinition(final String word)
		{
			if (this.descriptionFrame == null)
			{
				this.descriptionFrame = new JFrame("Description");
				this.descriptionFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				this.descriptionTabPanel = new JTabbedPane();
				this.descriptionFrame.add(this.descriptionTabPanel);
				this.descriptionFrame.setSize(600, 600);
				this.descriptionFrame.setLocationRelativeTo(null);
			}

			final Dictionary dictionary = this.grid.getDictionary();
			mutation:
			for (final String mutation : dictionary.getMutations(word))
			{
				// tests if definition already displayed
				final int tabCount = this.descriptionTabPanel.getTabCount();
				for (int i = 0; i < tabCount; i++)
				{
					if (this.descriptionTabPanel.getTitleAt(i).equals(mutation))
					{
						this.descriptionTabPanel.setSelectedIndex(i);
						continue mutation;
					}
				}

				Iterable<String> descriptions;
				try
				{
					descriptions = dictionary.getDescriptions(mutation);
				}
				catch (DictionaryException e)
				{
					descriptions = null;
				}

				final JPanel panel = new JPanel();
				panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

				if (descriptions == null)
				{
					panel.add(new JLabel(mutation + ": no definition found"));
				}
				else
				{
					descriptions.forEach(description -> panel.add(new JLabel(String.valueOf(description))));
				}

				final JScrollPane sp = new JScrollPane(panel);
				this.descriptionTabPanel.addTab(mutation, sp);
				this.descriptionTabPanel.setSelectedComponent(sp);

				this.descriptionFrame.setVisible(true);
				this.descriptionFrame.toFront();
			}
		}

		class StoneCell extends JComponent
		{
			private final Grid.Square square;
			private final AbstractAction showDefinitionAction;

			StoneCell(final int x, final int y)
			{
				this.square = JGrid.this.grid.getSquare(x, y);
				final JPopupMenu popup = new JPopupMenu();
				this.showDefinitionAction = new AbstractAction()
				{
					@Override
					public void actionPerformed(final ActionEvent e)
					{
						JGrid.this.grid.getWords(StoneCell.this.square).forEach(
								word -> showDefinition(word)
						);
					}
				};
				final JMenuItem menuItem = popup.add(this.showDefinitionAction);
				popup.addPopupMenuListener(new PopupMenuListener()
				{
					@Override
					public void popupMenuWillBecomeVisible(final PopupMenuEvent e)
					{
						final Set<String> words = JGrid.this.grid.getWords(StoneCell.this.square);
						if (words.isEmpty())
						{
							popup.remove(menuItem);
						}
						else
						{
							popup.add(menuItem);
							StoneCell.this.showDefinitionAction.putValue(Action.NAME, "Show definition" + (words.size() > 1 ? "s" : ""));
						}
					}

					@Override
					public void popupMenuWillBecomeInvisible(final PopupMenuEvent e)
					{
					}

					@Override
					public void popupMenuCanceled(final PopupMenuEvent e)
					{
					}
				});
				setComponentPopupMenu(popup);

				this.addMouseListener(new MouseAdapter()
				{
					@Override
					public void mouseClicked(final MouseEvent e)
					{
						if (JGrid.this.client != null && JGrid.this.client.isPlaying())
						{
							JGrid.this.client.setStartCell(StoneCell.this);
						}
					}
				});

			}

			@Override
			protected void paintComponent(final Graphics g)
			{
				super.paintComponent(g);

				final Graphics2D g2 = (Graphics2D) g;
				final Insets insets = getInsets();

				// Wir erben direkt aus JComponent und müssen darum den Background selbst zeichnen
				if (isOpaque() && getBackground() != null)
				{
					g2.setPaint(getBackground());
					g2.fillRect(insets.right, insets.top, getWidth() - insets.left, getHeight() - insets.bottom);
				}

				Stone stone;
				if (this.square.stone != null)
				{
					if (JGrid.this.hideNewStones && this.square.stone.getSettingAction() == JGrid.this.lastAction)
					{
						// don't draw
					}
					else
					{
						JStone.drawStone(g2, this, this.square.stone, Color.black);
					}
				}
				else if ((stone = JGrid.this.preparedMoveStones.get(this.square)) != null)
				{
					JStone.drawStone(g2, this, stone, Color.blue);
				}

				final MatteBorder specialBorder = JGrid.this.specialBorders.get(this.square);
				if (specialBorder != null)
				{
					specialBorder.paintBorder(
							this, g, 0, 0, getWidth(), getHeight()
					);
				}

				// Markiert die Start Zelle des Wortes
				if (JGrid.this.preparedMove != null && JGrid.this.preparedMove.startSquare == this.square)
				{
					g.setColor(Color.BLACK);
					final Polygon p = new Polygon();
					final int h = getHeight();
					final int POLYGONE_SIZE = h / 3;
					p.addPoint(-POLYGONE_SIZE / 2, 0);
					p.addPoint(0, POLYGONE_SIZE / 2);
					p.addPoint(POLYGONE_SIZE / 2, 0);

					final AffineTransform saved = ((Graphics2D) g).getTransform();
					switch (JGrid.this.preparedMove.getDirection())
					{
						case VERTICAL:
							g2.translate(h / 2f, 6f);
							break;
						case HORIZONTAL:
							g2.rotate(-Math.PI / 2);
							g2.translate(-h / 2f, 6f);
							break;
						default:
							throw new IllegalStateException("Unexpected value: " + JGrid.this.preparedMove.getDirection());
					}
					g.fillPolygon(p);
					((Graphics2D) g).setTransform(saved);

				}
			}

		}

		/**
		 * Component für die Anzeige der Nummer und Buchstaben der Zeilen und Spalten des Grids.
		 */
		private static class BorderCell extends JComponent
		{

			private final String label;

			BorderCell(final String label)
			{
				this.label = label;
			}

			@Override
			protected void paintComponent(final Graphics g)
			{
				super.paintComponent(g);

				final Graphics2D g2 = (Graphics2D) g;
				final Insets insets = getInsets();

				// Wir erben direkt aus JComponent und müssen darum den Background selbst zeichnen
				if (isOpaque() && getBackground() != null)
				{
					g2.setPaint(Color.lightGray);
					g2.fillRect(insets.right, insets.top, getWidth() - insets.left, getHeight() - insets.bottom);
				}

				// Draw the label
				g2.setColor(Color.BLACK);
				final Font font = g2.getFont().deriveFont(JStone.getCharacterSize(this)).deriveFont(Font.BOLD);
				g2.setFont(font);
				FontMetrics metrics = g.getFontMetrics(font);
				int tx = (getWidth() - metrics.stringWidth(this.label)) / 2;
				int ty = ((getHeight() - metrics.getHeight()) / 2) + metrics.getAscent();
				g.drawString(this.label, tx, ty);
			}
		}

		void setClient(final SwingClient client)
		{
			if (this.client != null)
			{
				throw new AssertionError("The client is already set");
			}
			this.client = client;
		}
	}

	/**
	 * Set a cell as the start of the future tipped word.
	 * @param cell
	 */
	private void setStartCell(final JGrid.StoneCell cell)
	{
		Move move = null;
		try
		{
			final String currentPrompt = this.commandPrompt.getText();
			move = Move.parseMove(getGrid(), currentPrompt, true);
			if (move.startSquare == cell.square)
			{
				move = move.getInvertedDirectionCopy();
			}
			else
			{
				move = move.getTranslatedCopy(cell.square);
			}
		}
		catch (ParseException e)
		{
			// OK: noch kein Prompt vorhanden.
		}

		if (move == null)
		{
			move = new Move(cell.square, Move.Direction.HORIZONTAL, "");
		}

		this.commandPrompt.setText(move.getNotation() + (move.word.isEmpty() ? " " : ""));

	}


	private class CommandPromptAction extends AbstractAction implements DocumentListener
	{

		static final String KEYWORD_HELP = "?";
		private Map<String, Command> commands = new LinkedHashMap<>();

		CommandPromptAction()
		{
			this.commands.put(KEYWORD_HELP, new Command("display help", (args -> {
				final StringBuffer sb = new StringBuffer();
				sb.append("<table border=1>");
				CommandPromptAction.this.commands.forEach(
						(k, c) -> sb.append("<tr><td>").append(k).append("</td><td>").append(c.description).append("</td></tr>"));
				sb.setLength(sb.length() - 1);
				sb.append("</table>");
				SwingClient.this.telnetFrame.appendConsoleText("blue", sb.toString(), false);
						return null;
					}))
			);

			this.commands.put("isvalid", new Command( "check if a word is valid", ( args -> {
				final String word = args[0];
				final Collection<String> mutations = SwingClient.this.game.getDictionary().getMutations(
						word.toUpperCase());
				final boolean isValid = mutations != null && !mutations.isEmpty();
				SwingClient.this.telnetFrame.appendConsoleText(
						isValid ? "blue" : "red",
						word + (isValid ? (" is valid " + mutations) : " is not valid"),
						true);
				return null;
			})));
		}

		@Override
		public void actionPerformed(final ActionEvent e)
		{
			String command = SwingClient.this.commandPrompt.getText();
			if (command.isEmpty())
			{
				return;
			}

			if (command.startsWith("/"))
			{
				final String[] splits = command.split("\\s+");
				String keyword = splits[0].substring(1).toLowerCase();
				if (!this.commands.containsKey(keyword))
				{
					keyword = KEYWORD_HELP;
				}
				Command c = this.commands.get(keyword);
				SwingClient.this.telnetFrame.appendConsoleText("black", "> " + command, false);
				c.action.apply(Arrays.copyOfRange(splits, 1, splits.length));
				return;
			}

			try
			{
				final Matcher m;
				if ((m = PATTERN_EXCHANGE_COMMAND.matcher(command)).matches())
				{
					final ArrayList<Character> chars = new ArrayList<>();
					for (final char c : m.group(1).toCharArray())
					{
						chars.add(c);
					}
					SwingClient.this.game.play(SwingClient.this, new Exchange(chars));
					SwingClient.this.commandPrompt.setText("");
				}
				else
				{
					final Move preparedMove = getPreparedMove();
					play(preparedMove);
				}
			}
			catch (final JokerPlacementException | ParseException ex)
			{
				onDispatchMessage(ex.getMessage());
				SwingClient.this.commandPrompt.setText("");
			}
		}

		private Move getPreparedMove() throws JokerPlacementException, ParseException
		{
			String command = SwingClient.this.commandPrompt.getText();
			final StringBuilder sb = new StringBuilder();


			boolean joker = false;
			for (final char c : command.toCharArray())
			{
				if (c == '*')
				{
					joker = true;
				}
				else
				{
					sb.append(joker ? Character.toLowerCase(c) : c);
					joker = false;
				}
			}

			final Pattern playCommandPattern = Pattern.compile("(?:play\\s+)?(.*)", Pattern.CASE_INSENSITIVE);
			Matcher matcher;
			Move move;
			if ((matcher = playCommandPattern.matcher(sb.toString())).matches())
			{
				final Rack rack;
				try
				{
					rack = SwingClient.this.game.getRack(SwingClient.this, SwingClient.this.playerKey);
				}
				catch (ScrabbleException e)
				{
					LOGGER.error(e);
					throw new JokerPlacementException("Error placing Joker", e);
				}
				final StringBuilder inputWord = new StringBuilder(matcher.group(1));
				move = Move.parseMove(SwingClient.this.game.getGrid(), inputWord.toString(), true);

				//
				// Check if jokers are needed and try to position them
				//

				LOGGER.debug("Word before positioning jokers: " + move.word);
				int remainingJokers = rack.countJoker();
				final HashSetValuedHashMap<Character, Integer> requiredLetters = new HashSetValuedHashMap<>();
				int i = inputWord.indexOf(" ") + 1;
				for (final Map.Entry<Grid.Square, Character> square : move.getSquares().entrySet())
				{
					if (square.getKey().isEmpty())
					{
						if (Character.isLowerCase(inputWord.charAt(i)))
						{
							remainingJokers--;
						}
						else
						{
							requiredLetters.put(square.getValue(), i);
						}
					}
					i++;
				}

				for (final Character letter : requiredLetters.keys())
				{
					final int inRack = rack.countLetter(letter);
					final int required = requiredLetters.get(letter).size();
					final int missing = required - inRack;
					if (missing > 0)
					{
						if (remainingJokers < missing)
						{
							throw new JokerPlacementException("No enough jokers", null);
						}

						if (missing == required)
						{
							for (final Integer pos : requiredLetters.get(letter))
							{
								inputWord.replace(pos, pos + 1, Character.toString(Character.toLowerCase(letter)));
							}
							remainingJokers -= missing;
						}
						else
						{
							throw new JokerPlacementException(
									"Cannot place the jokers: several emplacement possible. Use the *A notation.",
									null);
						}
					}
				}
				move = Move.parseMove(getGrid(), inputWord.toString(), true);
				LOGGER.debug("Word after having positioned white tiles: " + inputWord);
			}
			else
			{
				move = null;
			}
			return move;
		}


		@Override
		public void insertUpdate(final DocumentEvent e)
		{
			changedUpdate(e);
		}

		@Override
		public void removeUpdate(final DocumentEvent e)
		{
			changedUpdate(e);
		}

		@Override
		public void changedUpdate(final DocumentEvent e)
		{
			Move move;
			try
			{
				move = getPreparedMove();
			}
			catch (JokerPlacementException | ParseException e1)
			{
				LOGGER.debug(e1.getMessage());
				move = null;
			}

			SwingClient.this.jGrid.setPreparedMove(move);
			SwingClient.this.jGrid.repaint();
		}

		/**
		 * Ein Befehl und seine Antwort
		 */
		private class Command
		{
			final String description;
			final Function<String[], Void> action;

			private Command(final String description,
							final Function<String[], Void> action)
			{
				this.description = description;
				this.action = action;
			}
		}
	}

	private static class RackCell extends JComponent
	{
		private Stone stone;

		RackCell()
		{
			setPreferredSize(JStone.CELL_DIMENSION);
		}

		@Override
		protected void paintComponent(final Graphics g)
		{
			super.paintComponent(g);
			JStone.drawStone((Graphics2D) g, this, this.stone, Color.black);
		}

		public void setStone(final Stone stone)
		{
			this.stone = stone;
		}
	}

	/**
	 * Filter, das alles Eingetragene Uppercase schreibt
	 */
	private final static DocumentFilter UPPER_CASE_DOCUMENT_FILTER = new DocumentFilter()
	{
		public void insertString(DocumentFilter.FilterBypass fb, int offset,
								 String text, AttributeSet attr) throws BadLocationException
		{

			fb.insertString(offset, toUpperCase(text), attr);
		}

		public void replace(DocumentFilter.FilterBypass fb, int offset, int length,
							String text, AttributeSet attrs) throws BadLocationException
		{

			fb.replace(offset, length, toUpperCase(text), attrs);
		}

		/**
		 * Entfernt die Accente und liefert alles Uppercase.
		 * TODO: für Frz. sinnvoll, für Deutsch aber sicherlich nicht..
		 */
		private String toUpperCase(String text)
		{
			text = Normalizer.normalize(text, Normalizer.Form.NFD);
			text = text.replaceAll("[^\\p{ASCII}]", "");
			text = text.replaceAll("\\p{M}", "");
			return text.toUpperCase();
		}
	};

	/**
	 * Problem while placing joker.
	 */
	private static class JokerPlacementException extends Throwable
	{
		JokerPlacementException(final String message, final ScrabbleException e)
		{
			super(message, e);
		}
	}

	/**
	 * Eine Frame, die wie eine Telnet-Console sich immer erweiterndes Text anzeigt.
	 */
	private static class TelnetFrame
	{

		private final JLabel label;
		private final JFrame frame;

		TelnetFrame(final String title)
		{
			this.frame = new JFrame(title);
			this.label = new JLabel("<html>");
			this.label.setBorder(new BevelBorder(BevelBorder.LOWERED));
			this.frame.add(new JScrollPane(this.label, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
					JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
		}

		private void appendConsoleText(final String color, String text, final boolean escapeHtml)
		{
			this.label.setText(this.label.getText() + "\n<br><font color='" + color + "'>"
					+ (escapeHtml ? StringEscapeUtils.escapeHtml4(text) : text)
					+ "</font>");
			this.frame.setVisible(true);
		}

	}

	/**
	 * This action display the list of possible and authorized moves.
	 */
	private class PossibleMoveDisplayer extends AbstractAction
	{
		static final String LABEL_DISPLAY = "Display";
		static final String LABEL_HIDE = "Hide";

		private final BruteForceMethod bruteForceMethod;

		/** Group of buttons for the order */
		private final ButtonGroup orderButGroup;

		/** List of legal moves */
		private ArrayList<Grid.MoveMetaInformation> legalMoves;

		/** Swing list of sorted possible moves */
		private final JList<Grid.MoveMetaInformation> moveList;

		PossibleMoveDisplayer(final BruteForceMethod bruteForceMethod)
		{
			super(LABEL_DISPLAY);
			this.bruteForceMethod = bruteForceMethod;
			this.orderButGroup = new ButtonGroup();
			this.moveList = new JList<>();
			this.moveList.setCellRenderer(new DefaultListCellRenderer() {
				@Override
				public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus)
				{
					final Component label = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
					if (value instanceof Grid.MoveMetaInformation)
					{
						final Grid.MoveMetaInformation mmi = (Grid.MoveMetaInformation) value;
						this.setText(mmi.getMove().toString() + "  " + mmi.getScore()  + " pts");
					}
					return label;
				}
			});

			this.moveList.addListSelectionListener(event -> {
				Move move = null;
				for (int i = event.getFirstIndex(); i <= event.getLastIndex(); i++)
				{
					if (this.moveList.isSelectedIndex(i))
					{
						move = this.moveList.getModel().getElementAt(i).getMove();
						break;
					}
				}
				SwingClient.this.jGrid.setPreparedMove(move);
			});

			this.moveList.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseClicked(final MouseEvent e)
				{
					if (e.getClickCount() >= 2)
					{
						new SwingWorker<>()
						{
							@Override
							protected Object doInBackground() throws Exception
							{
								Thread.sleep(100);  // let time to object to be selected by other listener
								final List<Grid.MoveMetaInformation> selection = PossibleMoveDisplayer.this.moveList.getSelectedValuesList();
								if (selection.size() != 1)
								{
									return null;
								}

								final Move move = selection.get(0).getMove();
								play(move);

								return null;
							}
						}.execute();
					}
				}
			});
		}

		@Override
		public void actionPerformed(final ActionEvent e)
		{
			try
			{
				if (this.moveList.isDisplayable())
				{
					resetPossibleMovesPanel();
					SwingClient.this.showPossibilitiesButton.setText(LABEL_DISPLAY);
					return;
				}

				final Set<Move> moves = this.bruteForceMethod.getLegalMoves(getGrid(),
						SwingClient.this.game.getRack(SwingClient.this, SwingClient.this.playerKey));
				this.legalMoves = new ArrayList<>();

				for (final Move move : moves)
				{
					this.legalMoves.add(getGrid().getMetaInformation(move));
				}

				SwingClient.this.possibleMovePanel.add(
						new JScrollPane(this.moveList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED)
				);

				final JPanel orderMethodPanel = new JPanel();
				SwingClient.this.possibleMovePanel.add(orderMethodPanel, BorderLayout.NORTH);
				orderMethodPanel.add(new OrderButton("Score", Grid.MoveMetaInformation.SCORE_COMPARATOR));
				orderMethodPanel.add(new OrderButton("Length", Grid.MoveMetaInformation.WORD_LENGTH_COMPARATOR));
				this.orderButGroup.getElements().asIterator().next().doClick();
				SwingClient.this.possibleMovePanel.validate();
			}
			catch (ScrabbleException e1)
			{
				e1.printStackTrace();
			}

			SwingClient.this.showPossibilitiesButton.setText(LABEL_HIDE);
		}

		/**
		 * Radio button for the selection of the order of the word list.
		 */
		private class OrderButton extends JRadioButton
		{
			final Comparator<Grid.MoveMetaInformation> comparator;

			private OrderButton(final String label, final Comparator<Grid.MoveMetaInformation> comparator)
			{
				super();
				this.comparator = comparator;

				PossibleMoveDisplayer.this.orderButGroup.add(this);
				setAction(new AbstractAction(label)
				{
					@Override
					public void actionPerformed(final ActionEvent e)
					{
						PossibleMoveDisplayer.this.legalMoves.sort(OrderButton.this.comparator.reversed());
						PossibleMoveDisplayer.this.moveList.setListData(new Vector<>(PossibleMoveDisplayer.this.legalMoves));
					}
				});
			}
		}
	}

	private Grid getGrid()
	{
		return SwingClient.this.game.getGrid();
	}

	/**
	 * Play the move: inform the server about it and clear the client input field.
	 * @param move move to play
	 */
	private void play(final Move move)
	{
		this.game.play(SwingClient.this, move);
		this.commandPrompt.setText("");
		resetPossibleMovesPanel();
	}

	/**
	 *
	 */
	private class ExchangeTilesAction extends AbstractAction
	{
		ExchangeTilesAction()
		{
			super(
					"Exchange tiles",
					new ImageIcon(SwingClient.class.getResource("exchangeTiles.png"))
			);
		}

		@Override
		public void actionPerformed(final ActionEvent e)
		{
			final JFrame frame = new JFrame("Exchange");
			frame.setLayout(new BorderLayout());

			final JPanel carpet = new JPanel();
			carpet.setBackground(SCRABBLE_GREEN);
			final Dimension carpetDimension = new Dimension(250, 250);
			carpet.setPreferredSize(carpetDimension);
			carpet.setSize(carpetDimension);
			frame.add(carpet, BorderLayout.NORTH);
			frame.add(new JButton(new AbstractAction("Exchange them!")
			{
				@Override
				public void actionPerformed(final ActionEvent e)
				{
//					exchange(); TODO
					frame.dispose();
				}
			}), BorderLayout.SOUTH);

			frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			frame.setVisible(true);
			frame.setLocationRelativeTo(SwingClient.this.jRack);
			frame.pack();
		}
	}
}
