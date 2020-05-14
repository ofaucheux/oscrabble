package oscrabble.client;

import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.ScrabbleException;
import oscrabble.controller.Action;
import oscrabble.controller.Action.PlayTiles;
import oscrabble.controller.MicroServiceScrabbleServer;
import oscrabble.data.GameState;
import oscrabble.data.HistoryEntry;
import oscrabble.data.objects.Grid;
import oscrabble.data.objects.Square;
import oscrabble.player.AbstractPlayer;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.text.Normalizer;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Playground for Swing: grid and other components which are the same for all players.
 */
class Playground
{
	private final static int CELL_SIZE = 40;
	private static final Pattern PATTERN_EXCHANGE_COMMAND = Pattern.compile("-\\s*(.*)");
	private static final Pattern PATTERN_PASS_COMMAND = Pattern.compile("-\\s*");
	static final Color SCRABBLE_GREEN = Color.green.darker().darker();

	public static final ResourceBundle MESSAGES = SwingPlayer.MESSAGES;
	public static final Logger LOGGER = LoggerFactory.getLogger(Playground.class);

	/**
	 * Server
	 */
	private MicroServiceScrabbleServer server;

	/**
	 * Game ID
	 */
	private UUID game;


	/**
	 * Grid
	 */
	private final JGrid jGrid;

	/**
	 * Command prompt
	 */
	private final JTextField commandPrompt;

	/**
	 * Score board
	 */
	private final JScoreboard jScoreboard;

	private final TelnetFrame telnetFrame;

	/**
	 * Panel for the display of possible moves and corresponding buttons
	 */
	private final JPanel possibleMovePanel;

	/**
	 * Button to display / hide the possible moves
	 */
	private final JButton showPossibilitiesButton = null;  //TODO

	/**
	 * Thread executor
	 */
	private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(5);

	/**
	 * Future to let the last played stone flash
	 */
	private ScheduledFuture<Object> flashFuture;

	/**
	 * Listing of the history of the game
	 */
	private JList<HistoryEntry> historyList;

	/**
	 * Currently played play.
	 */
	private oscrabble.data.Action currentPlay;

	/**
	 * Registered Swing players
	 */
	private final LinkedList<SwingPlayer> swingPlayers = new LinkedList<>();

	/**
	 * The frame containing the grid (and other things)
	 */
	JFrame gridFrame;

	/**
	 * The client this playground is for
	 */
	private final Client client;

	Playground(final Client client)
	{
		this.client = client;
		this.jGrid = new JGrid();
		this.jGrid.setPlayground(this);
		this.jScoreboard = new JScoreboard(this);
		this.commandPrompt = new JTextField();
		final CommandPromptAction promptListener = new CommandPromptAction();
		this.commandPrompt.addActionListener(promptListener);
		this.commandPrompt.setFont(this.commandPrompt.getFont().deriveFont(20f));
		final AbstractDocument document = (AbstractDocument) this.commandPrompt.getDocument();
		document.addDocumentListener(promptListener);
		document.setDocumentFilter(UPPER_CASE_DOCUMENT_FILTER);
		telnetFrame = new TelnetFrame("Help");
		telnetFrame.frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		this.gridFrame = new JFrame();
		this.gridFrame.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(final WindowEvent e)
			{
				final int confirm = JOptionPane.showConfirmDialog(Playground.this.gridFrame, /*MESSAGES.getString(*/"quit.the.game"/*)*/, /*MESSAGES.getString(*/"confirm.quit"/*)*/, JOptionPane.YES_NO_OPTION);
				if (confirm == JOptionPane.YES_OPTION)
				{
					// TODO: schickt "ende" dem Server
//					Playground.this.game.setState(IGame.State.ENDED);
					Playground.this.gridFrame.dispose();
				}
			}
		});
		this.gridFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		this.gridFrame.setLayout(new BorderLayout());


		final JPanel mainPanel_01 = new JPanel();
		mainPanel_01.setLayout(new BoxLayout(mainPanel_01, BoxLayout.PAGE_AXIS));
		mainPanel_01.add(this.jGrid);
		mainPanel_01.add(this.commandPrompt);
		this.gridFrame.add(mainPanel_01);

		final JPanel eastPanel = new JPanel(new BorderLayout());
		final JPanel panel1 = new JPanel();
		panel1.setPreferredSize(new Dimension(200, 200));
		panel1.setLayout(new BoxLayout(panel1, BoxLayout.PAGE_AXIS));
		panel1.add(this.jScoreboard);
		panel1.add(Box.createVerticalGlue());

		final JPanel historyPanel = new JPanel(new BorderLayout());
		historyPanel.setBorder(new TitledBorder(MESSAGES.getString("moves")));
		this.historyList = new JList<>();
		this.historyList.setCellRenderer(new DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus)
			{
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				setText(((HistoryEntry) value).move);
				return this;
			}
		});
		final JScrollPane scrollPane = new JScrollPane(this.historyList);
		historyPanel.add(scrollPane);
		this.historyList.addPropertyChangeListener("model", (e) -> {   // scroll at end by content change
					SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(Integer.MAX_VALUE));
				}
		);

		// highlight the selected item in the list
		// TODO
//		this.historyList.addListSelectionListener(event -> {
//			if (event.getValueIsAdjusting())
//			{
//				return;
//			}
//			for (int index = event.getFirstIndex() ; index <= event.getLastIndex(); index++)
//			{
//				if (this.historyList.isSelectedIndex(index))
//				{
//					final HistoryEntry selected = this.historyList.getModel().getElementAt(index);
//					if (selected.isPlayTileAction())
//					{
//						final PlayTiles playTiles = selected.getPlayTiles();
//						this.jGrid.highlightWord(new ArrayList<>(playTiles.getSquares().keySet()));
//					}
//				}
//			}
//		});

		panel1.add(historyPanel);
		// todo: rollback
//		panel1.add(new JButton(new AbstractAction(MESSAGES.getString("rollback"))
//		{
//			@Override
//			public void actionPerformed(final ActionEvent e)
//			{
//				try
//				{
//					final SwingPlayer first = Playground.this.swingPlayers.getFirst();
//					Playground.this.game.rollbackLastMove(first, first.getPlayerKey());
//				}
//				catch (final Throwable ex)
//				{
//					LOGGER.error("Cannot play action " + e, ex);
//					JOptionPane.showMessageDialog(panel1, ex.toString());
//				}
//			}
//		}
//		));

		possibleMovePanel = new JPanel();
		possibleMovePanel.setBorder(new TitledBorder(MESSAGES.getString("possible.moves")));
		possibleMovePanel.setSize(new Dimension(200, 300));
		possibleMovePanel.setLayout(new BorderLayout());
		// todo: AI Empfehlungen
//		final BruteForceMethod bruteForceMethod = new BruteForceMethod(	this.game.getDictionary());
//		showPossibilitiesButton = new JButton(new PossibleMoveDisplayer(this, bruteForceMethod));
//		showPossibilitiesButton.setFocusable(false);
//		resetPossibleMovesPanel();

		panel1.add(possibleMovePanel);
		panel1.add(Box.createVerticalGlue());

		// todo: configpanel
//		final ConfigurationPanel configPanel = new ConfigurationPanel(
//				this.game.getConfiguration(),
//				null,
//				Collections.singleton("dictionary")
//		);
//		panel1.add(configPanel);
//		configPanel.setBorder(new TitledBorder(MESSAGES.getString("server.configuration")));
		eastPanel.add(panel1, BorderLayout.CENTER);
		this.gridFrame.add(eastPanel, BorderLayout.LINE_END);

		this.gridFrame.pack();
		this.gridFrame.setResizable(false);
		this.gridFrame.setVisible(true);


		SwingUtilities.invokeLater(() -> {
			this.gridFrame.requestFocus();
			this.commandPrompt.requestFocusInWindow();
			this.commandPrompt.grabFocus();
		});

		KeyboardFocusManager.setCurrentKeyboardFocusManager(new DefaultFocusManager()
		{
			@Override
			public boolean dispatchKeyEvent(final KeyEvent e)
			{
				// Snap the focus for the command prompt field
				if (!(e.getSource() instanceof JTextComponent))
				{
					Playground.this.commandPrompt.requestFocus();
					e.setSource(Playground.this.commandPrompt);
				}
				return super.dispatchKeyEvent(e);
			}
		});

	}

	private void resetPossibleMovesPanel()
	{
		possibleMovePanel.removeAll();
		possibleMovePanel.invalidate();
		possibleMovePanel.repaint();
		showPossibilitiesButton.setText(LABEL_DISPLAY);
		possibleMovePanel.add(showPossibilitiesButton, BorderLayout.SOUTH);
	}

	/**
	 * Execute action after play.
	 *
	 * @param caller the caller of the function
	 * @param play   the occurred play
	 */
	public void afterPlay(final SwingPlayer caller, final Action play)
	{
		if (caller != this.swingPlayers.getFirst())
		{
			return;
		}

//		this.jGrid.lastAction = action;
//		refreshUI();

		this.flashFuture = this.executor.schedule(
				() -> {
					for (int i = 0; i < 3; i++)
					{
						Thread.sleep(5 * 100);
						this.jGrid.hideNewStones = !this.jGrid.hideNewStones;
						this.jGrid.repaint();
					}
					this.jGrid.hideNewStones = false;
					this.jGrid.repaint();
					this.flashFuture = null;
					return null;
				},
				0,
				TimeUnit.SECONDS);

	}

	protected synchronized void refreshUI(final SwingPlayer caller)
	{
		if (!isFirstRegistered(caller))
		{
			return;
		}

		if (this.flashFuture != null)
		{
			this.flashFuture.cancel(true);
		}

		this.jGrid.repaint();
		// TODO?
//		this.jScoreboard.refresh();

//		TODO
//		final Iterable<GameState> history = this.server.getState(game)
//		this.historyList.setListData(IterableUtils.toList(history).toArray(new HistoryEntry[0]));
	}

	/**
	 * @param caller caller of the function
	 * @return {@code true} if the parameter is null or represents the first registered client.
	 */
	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	private boolean isFirstRegistered(final SwingPlayer caller)
	{
		return caller == null || caller == this.swingPlayers.getFirst();
	}

	/**
	 * Set the game.
	 *
	 */
	public void setGame()
	{
		// TODO: gerade dies ist wichtig.
//		this.game.addListener(new Game.GameListener()
//		{
//			private final CircularFifoQueue<Game.ScrabbleEvent> dummyQueue = new CircularFifoQueue<>(1);
//
//			@Override
//			public Queue<Game.ScrabbleEvent> getIncomingEventQueue()
//			{
//				return this.dummyQueue;
//			}
//
//			@Override
//			public void afterGameEnd()
//			{
//				Playground.this.executor.shutdown();
//			}
//		});
	}

	/**
	 * Register a player.
	 *
	 * @param swingPlayer player to register
	 */
	public void addPlayer(final SwingPlayer swingPlayer)
	{
		this.swingPlayers.add(swingPlayer);
	}

	public void onPlayRequired(final SwingPlayer caller)
	{
		if (!isFirstRegistered(caller))
		{
			return;
		}

//		for (final Map.Entry<IPlayerInfo, JScoreboard.ScorePanelLine> entry : this.jScoreboard.scoreLabels.entrySet())
//		{
//			final IPlayerInfo playerInfo = entry.getKey();
//			final JScoreboard.ScorePanelLine line = entry.getValue();
//			line.currentPlaying.setVisible(this.currentPlay != null && playerInfo.getName().equals(this.currentPlay.player.getName()));
//		}

		final Cursor cursor;
		if (true /* todo this.currentPlay.player instanceof SwingPlayer
				&& this.swingPlayers.contains(this.currentPlay.player)) */)
		{
			// ((SwingPlayer) this.currentPlay.player).updateRack();
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

	/**
	 * Players which UI has been created.
	 */
	private final Set<SwingPlayer> playersWithUI = new HashSet<>();

	/**
	 * Inform that the ui of a player has been created.
	 * @param player player
	 */
	public synchronized void afterUiCreated(final SwingPlayer player)
	{
		this.playersWithUI.add(player);
		final int numberSwingPlayers = getNumberSwingPlayers();
		if (this.playersWithUI.size() == numberSwingPlayers)
		{
			final int gap = 150;
			final int basePosX = this.gridFrame.getX() + this.gridFrame.getWidth();
			final int basePosY = this.gridFrame.getY() + (this.gridFrame.getHeight() / 2) - ( (gap + this.swingPlayers.get(0).rackFrame.getHeight()) * (numberSwingPlayers - 1) / 2);

			for (int i = 0; i < numberSwingPlayers; i++)
			{
				final JDialog rackFrame = this.swingPlayers.get(i).rackFrame;
				rackFrame.setLocation(
						basePosX,
						basePosY + gap * i
				);
			}
		}
	}

	/**
	 * @return the number of Swing Players registered for this playground.
	 */
	int getNumberSwingPlayers()
	{
		return this.swingPlayers.size();
	}

	private DisplayedMessage lastMessage;


	public void refreshUI(final GameState state)
	{
		this.jGrid.setGrid(state.getGrid());
		this.jScoreboard.updateDisplay(state.players, state.playerOnTurn);
	}

	public String getCommand()
	{
		return this.commandPrompt.getText();
	}

	public void clearPrompt()
	{
		this.commandPrompt.setText("");
	}

	/**
	 * Darstellung der Spielfläche
	 */
	static class JGrid extends JPanel
	{
		private final HashMap<Square, MatteBorder> specialBorders = new HashMap<>();

		private Grid grid;

		/**
		 *
		 */
//		private final Grid game;

		private final Map<Square, Character> preparedMoveStones;

		/**
		 * Frame für die Anzeige der Definition von Wärtern
		 */
		private final DictionaryComponent dictionaryComponent;

		final JComponent background;

		/**
		 * Client mit dem diese Grid verknüpft ist
		 */
		private Playground playground;

		/**
		 * Vorbereiteter Spielzug
		 */
		private PlayTiles preparedPlayTiles;

		/**
		 * Set to let the new stones flash
		 */
		private boolean hideNewStones;

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
			this.preparedMoveStones = new LinkedHashMap<>();
			final int size = 15 * CELL_SIZE;  // TODO: use a constant
			this.setPreferredSize(new Dimension(size, size));
			this.add(this.background);
		}

		void setGrid(oscrabble.data.Grid grid)
		{
			this.grid = new Grid(grid);
			final int numberOfRows = this.grid.getSize();

			final JPanel p1 = new JPanel();
			p1.setLayout(new GridLayout(numberOfRows, numberOfRows));

			// Draw each Cell
			for (int y = 0; y < numberOfRows; y++)
			{
				for (int x = 0; x < numberOfRows; x++)
				{
					final Square square = this.grid.get(x + 1, y + 1);
					if (square.isBorder)
					{
						p1.add(new BorderCell(square));
					}
					else
					{
						final JSquare cell = new JSquare(square);
						p1.add(cell);

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
							cellColor = SCRABBLE_GREEN;
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

//	todo
//		/**
//		 * Zeigt den vorbereiteten Spielzug auf dem Grid
//		 *
//		 * @param playTiles der Zug zu zeigen. {@code null} für gar keinen Zug.
//		 */
//		void highlightMove(final PlayTiles playTiles)
//		{
//			this.preparedPlayTiles = playTiles;
//			this.preparedMoveStones.clear();
//			if (playTiles != null)
//			{
//				final ArrayList<Grid.Square> squares = new ArrayList<>(this.preparedMoveStones.keySet());
//				this.preparedMoveStones.putAll(playTiles.getStones(this.grid, this.sli));
//				highlightWord(squares);
//			}
//		}

		private void highlightWord(final ArrayList<Square> squares)
		{
			final int INSET = 4;
			final Color preparedMoveColor = Color.RED;
			this.specialBorders.clear();

			if (!squares.isEmpty())
			{
				boolean isHorizontal = squares.size() == 1
						|| squares.get(1).getX() == squares.get(0).getX() + 1;

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
				dictionaryFrame = new JFrame(MESSAGES.getString("description"));
				dictionaryFrame.add(this.dictionaryComponent);
				dictionaryFrame.setSize(600, 600);
			}


			this.dictionaryComponent.showDescription(word);
			dictionaryFrame.setVisible(true);
			dictionaryFrame.toFront();
		}

		class JSquare extends JComponent
		{
			private final AbstractAction showDefinitionAction;
			private final Square square;

			JSquare(final Square square)
			{
				this.square = square;
				final JPopupMenu popup = new JPopupMenu();
				this.showDefinitionAction = new AbstractAction()
				{
					@Override
					public void actionPerformed(final ActionEvent e)
					{
						JGrid.this.grid.getWords(square.getCoordinate()).forEach(
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
						final Set<String> words = JGrid.this.grid.getWords(square.getCoordinate());
						if (words.isEmpty())
						{
							popup.remove(menuItem);
						}
						else
						{
							popup.add(menuItem);
							JSquare.this.showDefinitionAction.putValue(javax.swing.Action.NAME, (words.size() > 1 ? MESSAGES.getString("show.definitions") : MESSAGES.getString("show.definition")));
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
						if (JGrid.this.playground != null)
						{
//							JGrid.this.playground.setStartCell(JSquare.this); todo
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

				JTile tile;
				Character c;
				if (this.square.tile != null)
				{
					tile = new JTile(this.square.tile);
					//noinspection StatementWithEmptyBody
					if (JGrid.this.hideNewStones && this.square.action == JGrid.this.lastAction)
					{
						// don't draw
					}
					else
					{
						tile.paintComponent(g2);
					}
				}
				else if ((c = preparedMoveStones.get(this.square)) != null)
				{
					// TODO
//					JTile.drawTile(g2, this, /* TODO tiles.get(c) */ null, Color.blue);
				}

				final MatteBorder specialBorder = JGrid.this.specialBorders.get(this.square);
				if (specialBorder != null)
				{
					specialBorder.paintBorder(
							this, g, 0, 0, getWidth(), getHeight()
					);
				}

				// Markiert die Start Zelle des Wortes todo
//				if (JGrid.this.preparedPlayTiles != null && JGrid.this.preparedPlayTiles.startSquare == this.square)
//				{
//					g.setColor(Color.BLACK);
//					final Polygon p = new Polygon();
//					final int h = getHeight();
//					final int POLYGONE_SIZE = h / 3;
//					p.addPoint(-POLYGONE_SIZE / 2, 0);
//					p.addPoint(0, POLYGONE_SIZE / 2);
//					p.addPoint(POLYGONE_SIZE / 2, 0);
//
//					final AffineTransform saved = ((Graphics2D) g).getTransform();
//					switch (JGrid.this.preparedPlayTiles.getDirection())
//					{
//						case Grid.Direction.VERTICAL:
//							g2.translate(h / 2f, 6f);
//							break;
//						case Grid.Direction.HORIZONTAL:
//							g2.rotate(-Math.PI / 2);
//							g2.translate(-h / 2f, 6f);
//							break;
//						default:
//							throw new IllegalStateException("Unexpected value: " + JGrid.this.preparedPlayTiles.getDirection());
//					}
//					g.fillPolygon(p);
//					((Graphics2D) g).setTransform(saved);
//
//				}
			}

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

				// Wir erben direkt aus JComponent und müssen darum den Background selbst zeichnen
				if (isOpaque() && getBackground() != null)
				{
					g2.setPaint(Color.lightGray);
					g2.fillRect(insets.right, insets.top, getWidth() - insets.left, getHeight() - insets.bottom);
				}

				// Draw the label
				g2.setColor(Color.BLACK);
				final Font font = g2.getFont().deriveFont(JTile.getCharacterSize(this)).deriveFont(Font.BOLD);
				g2.setFont(font);
				FontMetrics metrics = g.getFontMetrics(font);
				final String label = square.getX() == 0 ? Integer.toString(square.getY()) : Character.toString((char) ('A' + square.getY() - 1));
				int tx = (getWidth() - metrics.stringWidth(label)) / 2;
				int ty = ((getHeight() - metrics.getHeight()) / 2) + metrics.getAscent();
				g.drawString(label, tx, ty);
			}
		}

		void setPlayground(final Playground client)
		{
			if (this.playground != null)
			{
				throw new AssertionError("The client is already set");
			}
			this.playground = client;
		}
	}

	/**
	 * Play the move: inform the server about it and clear the client input field.
	 *
	 * @param playTiles move to play
	 */
	private void play(final AbstractPlayer playerID, final PlayTiles playTiles) throws ScrabbleException
	{
		// TODO
//		final SwingPlayer player = getCurrentSwingPlayer();
//		assert player != null;
		this.server.play(this.game, playerID.buildAction(playTiles.notation));
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
		 * Entfernt die Umlaute und liefert alles Uppercase.
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

	static final String LABEL_DISPLAY = MESSAGES.getString("show.possibilities");
	static final String LABEL_HIDE = MESSAGES.getString("hide.possibilities");

	/**
	 * Set a cell as the start of the future tipped word. todo
	 *
//	 * @param cell Cell
	 */
//	private void setStartCell(final JGrid.JSquare cell)
//	{
//		PlayTiles playTiles = null;
//		try
//		{
//			final String currentPrompt = this.commandPrompt.getText();
//			final oscrabble.controller.Action action = Action.parse(currentPrompt);
//			if (action instanceof PlayTiles)
//			{
//				playTiles = (PlayTiles) action;
//				if (playTiles.startSquare.getNotation().equals(cell.square.getCoordinate()))
//				{
//					playTiles = playTiles.getInvertedDirectionCopy();
//				}
//				else
//				{
//					playTiles = playTiles.getTranslatedCopy(cell.square);
//				}
//			}
//		}
//		catch (ParseException | ScrabbleException.ForbiddenPlayException e)
//		{
//			// OK: noch kein Prompt vorhanden, oder nicht parsable.
//		}
//
//		if (playTiles == null)
//		{
//			playTiles = new PlayTiles(cell.square, Grid.Direction.HORIZONTAL, "");
//		}
//
//		this.commandPrompt.setText(playTiles.getNotation() + (playTiles.word.isEmpty() ? " " : ""));
//
//	}


//	/**
//	 * @return the current player or {@code null} when current not Swing one
//	 */
//	private SwingPlayer getCurrentSwingPlayer()
//	{
//		if (!(this.currentPlay.player instanceof SwingPlayer))
//		{
//			return null;
//		}
//		return (SwingPlayer) this.currentPlay.player;
//	}

	private class CommandPromptAction extends AbstractAction implements DocumentListener
	{

		static final String KEYWORD_HELP = "?";
		private Map<String, Command> commands = new LinkedHashMap<>();

		@Override
		public void actionPerformed(final ActionEvent e)
		{
			if (client == null)
			{
				JOptionPane.showMessageDialog(Playground.this.gridFrame, "This playground has no client");
				return;
			}

			Playground.this.client.executeCommand(commandPrompt.getText());
			commandPrompt.setText("");
		}

		private oscrabble.controller.Action getPreparedMove() throws Playground.JokerPlacementException, ParseException, ScrabbleException.ForbiddenPlayException
		{
			// TODO
//			final SwingPlayer player = getCurrentSwingPlayer();
//			if (player == null)
//			{
//				throw new IllegalStateException("Player is not current one");
//			}

			String command = Playground.this.commandPrompt.getText();
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
			oscrabble.controller.Action action;
			if ((matcher = playCommandPattern.matcher(sb.toString())).matches())
			{
				final JRackCell rack;
//				try
//				{
//					rack = Playground.this.game.getRack(player, player.getPlayerKey());
//				}
//				catch (ScrabbleException e)
//				{
//					LOGGER.error(e.toString(), e);
//					throw new JokerPlacementException(MESSAGES.getString("error.placing.joker"), e);
//				}
				final StringBuilder inputWord = new StringBuilder(matcher.group(1));
				action = oscrabble.controller.Action.parse(inputWord.toString());

//				//
//				// todo Check if jokers are needed and try to position them
//				//
//				if (action instanceof PlayTiles)
//				{
//					final PlayTiles playTiles = (PlayTiles) action;
//					LOGGER.debug("Word before positioning jokers: " + playTiles.word);
//					int remainingJokers = rack.countJoker();
//					final HashSetValuedHashMap<Character, Integer> requiredLetters = new HashSetValuedHashMap<>();
//					int i = inputWord.indexOf(" ") + 1;
//					for (final Map.Entry<Grid.Square, Character> square : playTiles.getSquares().entrySet())
//					{
//						if (square.getKey().isEmpty())
//						{
//							if (Character.isLowerCase(inputWord.charAt(i)))
//							{
//								remainingJokers--;
//							}
//							else
//							{
//								requiredLetters.put(square.getValue(), i);
//							}
//						}
//						i++;
//					}
//
//					for (final Character letter : requiredLetters.keys())
//					{
//						final int inRack = rack.countLetter(letter);
//						final int required = requiredLetters.get(letter).size();
//						final int missing = required - inRack;
//						if (missing > 0)
//						{
//							if (remainingJokers < missing)
//							{
//								throw new JokerPlacementException(MESSAGES.getString("no.enough.jokers"), null);
//							}
//
//							if (missing == required)
//							{
//								for (final Integer pos : requiredLetters.get(letter))
//								{
//									inputWord.replace(pos, pos + 1, Character.toString(Character.toLowerCase(letter)));
//								}
//								remainingJokers -= missing;
//							}
//							else
//							{
//								throw new JokerPlacementException(
//										MESSAGES.getString("cannot.place.the.jokers.several.emplacement.possible.use.the.a.notation"),
//										null);
//							}
//						}
//					}
//				}
				action = Action.parse(inputWord.toString());
				LOGGER.debug("Word after having positioned white tiles: " + inputWord);
			}
			else
			{
				action = null;
			}
			return action;
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
			try
			{
				getPreparedMove();
			}
			catch (Playground.JokerPlacementException | ParseException | ScrabbleException.ForbiddenPlayException e1)
			{
				LOGGER.debug(e1.getMessage());
			}

			Playground.this.jGrid.repaint();
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



	/**
	 * A message and the time it was displayed.
	 */
	private final static class DisplayedMessage
	{
		Object message;
		long displayTime;
	}
}
