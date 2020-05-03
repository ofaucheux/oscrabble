package oscrabble.client;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.log4j.Logger;
import oscrabble.Grid;
import oscrabble.Rack;
import oscrabble.ScrabbleException;
import oscrabble.server.action.Action;
import oscrabble.server.action.Exchange;
import oscrabble.server.action.PlayTiles;
import oscrabble.server.action.SkipTurn;
import oscrabble.configuration.ConfigurationPanel;
import oscrabble.dictionary.DictionaryComponent;
import oscrabble.dictionary.ScrabbleLanguageInformation;
import oscrabble.dictionary.Tile;
import oscrabble.player.BruteForceMethod;
import oscrabble.server.Game;
import oscrabble.server.IGame;
import oscrabble.server.IPlayerInfo;
import oscrabble.server.Play;

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
import java.awt.geom.AffineTransform;
import java.text.Normalizer;
import java.text.ParseException;
import java.util.List;
import java.util.Queue;
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
	public static final Logger LOGGER = Logger.getLogger(Playground.class);
	private static final Pattern PATTERN_EXCHANGE_COMMAND = Pattern.compile("-\\s*(.*)");
	private static final Pattern PATTERN_PASS_COMMAND = Pattern.compile("-\\s*");
	static final Color SCRABBLE_GREEN = Color.green.darker().darker();

	public static final ResourceBundle MESSAGES = Game.MESSAGES;

	/**
	 * The game
	 */
	private IGame game;

	/**
	 * Grid
	 */
	JGrid jGrid;

	/**
	 * Command prompt
	 */
	private JTextField commandPrompt;

	/**
	 * Score board
	 */
	private JScoreboard jScoreboard;

	private static TelnetFrame telnetFrame;

	/**
	 * Panel for the display of possible moves and corresponding buttons
	 */
	private static JPanel possibleMovePanel;

	/**
	 * Button to display / hide the possible moves
	 */
	private static JButton showPossibilitiesButton;

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
	private JList<Game.HistoryEntry> historyList;

	/**
	 * Currently played play.
	 */
	private Play currentPlay;

	/**
	 * Registered Swing players
	 */
	private final LinkedList<SwingPlayer> swingPlayers = new LinkedList<>();

	/**
	 * The frame containing the grid (and other things)
	 */
	JFrame gridFrame;

	Playground()
	{
	}


	/**
	 * create UI and display it
	 */
	void display()
	{
		assert this.jGrid == null;

		this.jGrid = new JGrid(getGrid(), this.game.getScrabbleLanguageInformation(), this.game);
		this.jGrid.setClient(this);
		this.jScoreboard = new JScoreboard(this.game);
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
				final int confirm = JOptionPane.showConfirmDialog(Playground.this.gridFrame, MESSAGES.getString("quit.the.game"), MESSAGES.getString("confirm.quit"), JOptionPane.YES_NO_OPTION);
				if (confirm == JOptionPane.YES_OPTION)
				{
					Playground.this.game.setState(IGame.State.ENDED);
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
				setText(((Game.HistoryEntry) value).formatAsString());
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
		this.historyList.addListSelectionListener(event -> {
			if (event.getValueIsAdjusting())
			{
				return;
			}
			for (int index = event.getFirstIndex() ; index <= event.getLastIndex(); index++)
			{
				if (this.historyList.isSelectedIndex(index))
				{
					final Game.HistoryEntry selected = this.historyList.getModel().getElementAt(index);
					if (selected.isPlayTileAction())
					{
						final PlayTiles playTiles = selected.getPlayTiles();
						this.jGrid.highlightWord(new ArrayList<>(playTiles.getSquares().keySet()));
					}
				}
			}
		});

		panel1.add(historyPanel);
		panel1.add(new JButton(new AbstractAction(MESSAGES.getString("rollback"))
		{
			@Override
			public void actionPerformed(final ActionEvent e)
			{
				try
				{
					final SwingPlayer first = Playground.this.swingPlayers.getFirst();
					Playground.this.game.rollbackLastMove(first, first.getPlayerKey());
				}
				catch (final Throwable ex)
				{
					LOGGER.error(ex, ex);
					JOptionPane.showMessageDialog(panel1, ex.toString());
				}
			}
		}
		));

		possibleMovePanel = new JPanel();
		possibleMovePanel.setBorder(new TitledBorder(MESSAGES.getString("possible.moves")));
		possibleMovePanel.setSize(new Dimension(200, 300));
		possibleMovePanel.setLayout(new BorderLayout());
		final BruteForceMethod bruteForceMethod = new BruteForceMethod(	this.game.getDictionary());
		showPossibilitiesButton = new JButton(new PossibleMoveDisplayer(bruteForceMethod));
		showPossibilitiesButton.setFocusable(false);
		resetPossibleMovesPanel();

		panel1.add(possibleMovePanel);
		panel1.add(Box.createVerticalGlue());

		final ConfigurationPanel configPanel = new ConfigurationPanel(
				this.game.getConfiguration(),
				null,
				Collections.singleton("dictionary")
		);
		panel1.add(configPanel);
		configPanel.setBorder(new TitledBorder(MESSAGES.getString("server.configuration")));
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

	/**
	 * @return grid of the game.
	 */
	private Grid getGrid()
	{
		return this.game.getGrid();
	}

	private static void resetPossibleMovesPanel()
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
	public void afterPlay(final SwingPlayer caller, final Play play)
	{
		if (caller != this.swingPlayers.getFirst())
		{
			return;
		}

		this.jGrid.lastAction = play.action;
		refreshUI(null);

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
		this.jScoreboard.refreshDisplay();

		final Iterable<Game.HistoryEntry> history = this.game.getHistory();
		this.historyList.setListData(IterableUtils.toList(history).toArray(new Game.HistoryEntry[0]));
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
	 * @param game game
	 */
	public void setGame(final IGame game)
	{
		if (this.game != null && this.game != game)
		{
			throw new AssertionError("Game already set");
		}
		this.game = game;
		this.game.addListener(new Game.GameListener()
		{
			private final CircularFifoQueue<Game.ScrabbleEvent> dummyQueue = new CircularFifoQueue<>(1);

			@Override
			public Queue<Game.ScrabbleEvent> getIncomingEventQueue()
			{
				return this.dummyQueue;
			}

			@Override
			public void afterGameEnd()
			{
				Playground.this.executor.shutdown();
			}
		});
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

	public synchronized void beforeGameStart()
	{
		if (this.gridFrame != null)
		{
			return;
		}
		display();
		this.jScoreboard.prepareBoard();
	}

	public void onPlayRequired(final SwingPlayer caller, final Play play)
	{
		if (!isFirstRegistered(caller))
		{
			return;
		}

		this.currentPlay = play;
		for (final Map.Entry<IPlayerInfo, Playground.JScoreboard.ScorePanelLine> entry : this.jScoreboard.scoreLabels.entrySet())
		{
			final IPlayerInfo playerInfo = entry.getKey();
			final Playground.JScoreboard.ScorePanelLine line = entry.getValue();
			line.currentPlaying.setVisible(this.currentPlay != null && playerInfo.getName().equals(this.currentPlay.player.getName()));
		}

		final Cursor cursor;
		if (this.currentPlay.player instanceof SwingPlayer
				&& this.swingPlayers.contains(this.currentPlay.player))
		{
			((SwingPlayer) this.currentPlay.player).updateRack();
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

	/**
	 * Panel for the display of the actual score.
	 */
	private class JScoreboard extends JPanel
	{
		private final IGame game;
		private final HashMap<IPlayerInfo, ScorePanelLine> scoreLabels = new HashMap<>();

		JScoreboard(final IGame game)
		{
			this.game = game;
			setPreferredSize(new Dimension(200, 0));
			setLayout(new GridBagLayout());
			setBorder(new TitledBorder("Score"));
		}

		void refreshDisplay()
		{
			for (final IPlayerInfo playerInfo : this.game.getPlayers())
			{
				this.scoreLabels.get(playerInfo).score.setText(playerInfo.getScore() + " pts");
			}
		}

		void prepareBoard()
		{
			final double SMALL_WEIGHT = 0.1;
			final double BIG_WEIGHT = 10;

			final Dimension buttonDim = new Dimension(20, 20);
			final List<IPlayerInfo> players = this.game.getPlayers();
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
								JScoreboard.this.game.editParameters(Playground.this.swingPlayers.getFirst().getPlayerKey(), player);
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

	private DisplayedMessage lastMessage;

	/**
	 * Display a message. It will not be displayed if the same message has been displayed some seconds ago and no other one since.
	 *
	 * @param message message to display
	 */
	void showMessage(final Object message)
	{
		if (this.lastMessage != null
				&& this.lastMessage.message.equals(message)
				&& System.currentTimeMillis() < this.lastMessage.displayTime + 5000)
		{
			return;
		}

		this.lastMessage = new DisplayedMessage();
		this.lastMessage.message = message;
		this.lastMessage.displayTime = System.currentTimeMillis();

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
		private final ScrabbleLanguageInformation sli;
		/**
		 *
		 */
		private final IGame game;

		private final Map<Grid.Square, Tile> preparedMoveStones;

		/**
		 * Frame für die Anzeige der Definition von Wärtern
		 */
		private final DictionaryComponent dictionaryComponent;

		final JComponent background;

		/**
		 * Client mit dem diese Grid verknüpft ist
		 */
		private Playground client;

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
		private Action lastAction;


		/**
		 * Spielfeld des Scrabbles
		 */
		JGrid(final Grid grid, final ScrabbleLanguageInformation sli, final IGame game)
		{
			this.grid = grid;
			this.game = game;
			final int numberOfRows = grid.getSize() + 2;
			this.sli = sli;
			this.dictionaryComponent = new DictionaryComponent(game);

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
							case Bonus.NONE:
								cellColor = SCRABBLE_GREEN;
								break;
							case Bonus.BORDER:
								cellColor = Color.black;
								break;
							case Bonus.LIGHT_BLUE:
								cellColor = Color.decode("0x00BFFF");
								break;
							case Bonus.DARK_BLUE:
								cellColor = Color.blue;
								break;
							case Bonus.RED:
								cellColor = Color.red;
								break;
							case Bonus.ROSE:
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
		 *
		 * @param playTiles der Zug zu zeigen. {@code null} für gar keinen Zug.
		 */
		void highlightMove(final PlayTiles playTiles)
		{
			this.preparedPlayTiles = playTiles;
			this.preparedMoveStones.clear();
			if (playTiles != null)
			{
				final ArrayList<Grid.Square> squares = new ArrayList<>(this.preparedMoveStones.keySet());
				this.preparedMoveStones.putAll(playTiles.getStones(this.grid, this.sli));
				highlightWord(squares);
			}
		}

		private void highlightWord(final ArrayList<Grid.Square> squares)
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


			for (final String mutation : this.game.getMutations(word))
			{
				this.dictionaryComponent.showDescription(mutation);
			}
			dictionaryFrame.setVisible(true);
			dictionaryFrame.toFront();
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
							StoneCell.this.showDefinitionAction.putValue(javax.swing.Action.NAME, (words.size() > 1 ? MESSAGES.getString("show.definitions") : MESSAGES.getString("show.definition")));
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
						if (JGrid.this.client != null)
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

				Tile tile;
				if (this.square.tile != null)
				{
					//noinspection StatementWithEmptyBody
					if (JGrid.this.hideNewStones && JGrid.this.game.getSettingAction(this.square.tile) == JGrid.this.lastAction)
					{
						// don't draw
					}
					else
					{
						JTile.drawStone(g2, this, this.square.tile, Color.black);
					}
				}
				else if ((tile = JGrid.this.preparedMoveStones.get(this.square)) != null)
				{
					JTile.drawStone(g2, this, tile, Color.blue);
				}

				final MatteBorder specialBorder = JGrid.this.specialBorders.get(this.square);
				if (specialBorder != null)
				{
					specialBorder.paintBorder(
							this, g, 0, 0, getWidth(), getHeight()
					);
				}

				// Markiert die Start Zelle des Wortes
				if (JGrid.this.preparedPlayTiles != null && JGrid.this.preparedPlayTiles.startSquare == this.square)
				{
					g.setColor(Color.BLACK);
					final Polygon p = new Polygon();
					final int h = getHeight();
					final int POLYGONE_SIZE = h / 3;
					p.addPoint(-POLYGONE_SIZE / 2, 0);
					p.addPoint(0, POLYGONE_SIZE / 2);
					p.addPoint(POLYGONE_SIZE / 2, 0);

					final AffineTransform saved = ((Graphics2D) g).getTransform();
					switch (JGrid.this.preparedPlayTiles.getDirection())
					{
						case Direction.VERTICAL:
							g2.translate(h / 2f, 6f);
							break;
						case Direction.HORIZONTAL:
							g2.rotate(-Math.PI / 2);
							g2.translate(-h / 2f, 6f);
							break;
						default:
							throw new IllegalStateException("Unexpected value: " + JGrid.this.preparedPlayTiles.getDirection());
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
				final Font font = g2.getFont().deriveFont(JTile.getCharacterSize(this)).deriveFont(Font.BOLD);
				g2.setFont(font);
				FontMetrics metrics = g.getFontMetrics(font);
				int tx = (getWidth() - metrics.stringWidth(this.label)) / 2;
				int ty = ((getHeight() - metrics.getHeight()) / 2) + metrics.getAscent();
				g.drawString(this.label, tx, ty);
			}
		}

		void setClient(final Playground client)
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
	 *
	 * @param cell Cell
	 */
	private void setStartCell(final JGrid.StoneCell cell)
	{
		PlayTiles playTiles = null;
		try
		{
			final String currentPrompt = this.commandPrompt.getText();
			final Action action = PlayTiles.parseMove(getGrid(), currentPrompt, true);
			if (action instanceof PlayTiles)
			{
				playTiles = (PlayTiles) action;
				if (playTiles.startSquare == cell.square)
				{
					playTiles = playTiles.getInvertedDirectionCopy();
				}
				else
				{
					playTiles = playTiles.getTranslatedCopy(cell.square);
				}
			}
		}
		catch (ParseException e)
		{
			// OK: noch kein Prompt vorhanden.
		}

		if (playTiles == null)
		{
			playTiles = new PlayTiles(cell.square, PlayTiles.Direction.HORIZONTAL, "");
		}

		this.commandPrompt.setText(playTiles.getNotation() + (playTiles.word.isEmpty() ? " " : ""));

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
						telnetFrame.appendConsoleText("blue", sb.toString(), false);
						return null;
					}))
			);

			this.commands.put("isValid", new Command("check if a word is valid", (args -> {
				final String word = args[0];
				final Collection<String> mutations = Playground.this.game.getDictionary().getMutations(
						word.toUpperCase());
				final boolean isValid = mutations != null && !mutations.isEmpty();
				telnetFrame.appendConsoleText(
						isValid ? "blue" : "red",
						word + (isValid ? (" is valid " + mutations) : " is not valid"),
						true);
				return null;
			})));
		}

		@Override
		public void actionPerformed(final ActionEvent e)
		{
			String command = Playground.this.commandPrompt.getText();
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
				telnetFrame.appendConsoleText("black", "> " + command, false);
				c.action.apply(Arrays.copyOfRange(splits, 1, splits.length));
				return;
			}

			try
			{
				final SwingPlayer swingPlayer = getCurrentSwingPlayer();
				if (swingPlayer != null && swingPlayer == Playground.this.currentPlay.player)
				{
					final Matcher m;
					if ((m = PATTERN_EXCHANGE_COMMAND.matcher(command)).matches())
					{
						Playground.this.game.play(swingPlayer.getPlayerKey(), Playground.this.currentPlay, new Exchange(m.group(1)));
					}
					else if (PATTERN_PASS_COMMAND.matcher(command).matches())
					{
						Playground.this.game.play(swingPlayer.getPlayerKey(), Playground.this.currentPlay, SkipTurn.SINGLETON);
					}
					else
					{
						final PlayTiles preparedPlayTiles = ((PlayTiles) getPreparedMove());
						play(preparedPlayTiles);
					}
					Playground.this.commandPrompt.setText("");
					resetPossibleMovesPanel();
				}
				else
				{
					JOptionPane.showMessageDialog(Playground.this.jGrid, MESSAGES.getString("it.s.not.your.turn"));
				}
			}
			catch (final JokerPlacementException | ParseException | ScrabbleException.NotInTurn | ScrabbleException.InvalidSecretException ex)
			{
				showMessage(ex.getMessage());
				Playground.this.commandPrompt.setText("");
			}
		}

		private Action getPreparedMove() throws JokerPlacementException, ParseException
		{
			final SwingPlayer player = getCurrentSwingPlayer();
			if (player == null)
			{
				throw new IllegalStateException("Player is not current one");
			}

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
			Action action;
			if ((matcher = playCommandPattern.matcher(sb.toString())).matches())
			{
				final Rack rack;
				try
				{
					rack = Playground.this.game.getRack(player, player.getPlayerKey());
				}
				catch (ScrabbleException e)
				{
					LOGGER.error(e);
					throw new JokerPlacementException(MESSAGES.getString("error.placing.joker"), e);
				}
				final StringBuilder inputWord = new StringBuilder(matcher.group(1));
				action = PlayTiles.parseMove(Playground.this.game.getGrid(), inputWord.toString(), true);

				//
				// Check if jokers are needed and try to position them
				//
				if (action instanceof PlayTiles)
				{
					final PlayTiles playTiles = (PlayTiles) action;
					LOGGER.debug("Word before positioning jokers: " + playTiles.word);
					int remainingJokers = rack.countJoker();
					final HashSetValuedHashMap<Character, Integer> requiredLetters = new HashSetValuedHashMap<>();
					int i = inputWord.indexOf(" ") + 1;
					for (final Map.Entry<Grid.Square, Character> square : playTiles.getSquares().entrySet())
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
								throw new JokerPlacementException(MESSAGES.getString("no.enough.jokers"), null);
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
										MESSAGES.getString("cannot.place.the.jokers.several.emplacement.possible.use.the.a.notation"),
										null);
							}
						}
					}
				}
				action = PlayTiles.parseMove(getGrid(), inputWord.toString(), true);
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
			catch (JokerPlacementException | ParseException e1)
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

	static class RackCell extends JComponent
	{
		private Tile tile;

		RackCell()
		{
			setPreferredSize(JTile.CELL_DIMENSION);
		}

		@Override
		protected void paintComponent(final Graphics g)
		{
			super.paintComponent(g);
			JTile.drawStone((Graphics2D) g, this, this.tile, Color.black);
		}

		public void setTile(final Tile tile)
		{
			this.tile = tile;
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
	 * This action display the list of possible and authorized moves.
	 */
	private class PossibleMoveDisplayer extends AbstractAction
	{

		private final BruteForceMethod bruteForceMethod;

		/**
		 * Group of buttons for the order
		 */
		private final ButtonGroup orderButGroup;

		/**
		 * List of legal moves
		 */
		private ArrayList<Grid.MoveMetaInformation> legalMoves;

		/**
		 * Swing list of sorted possible moves
		 */
		private final JList<Grid.MoveMetaInformation> moveList;

		PossibleMoveDisplayer(final BruteForceMethod bruteForceMethod)
		{
			super(LABEL_DISPLAY);
			this.bruteForceMethod = bruteForceMethod;
			this.orderButGroup = new ButtonGroup();
			this.moveList = new JList<>();
			this.moveList.setCellRenderer(new DefaultListCellRenderer()
			{
				@Override
				public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus)
				{
					final Component label = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
					if (value instanceof Grid.MoveMetaInformation)
					{
						final Grid.MoveMetaInformation mmi = (Grid.MoveMetaInformation) value;
						this.setText(mmi.getPlayTiles().toString() + "  " + mmi.getScore() + " pts");
					}
					return label;
				}
			});

			this.moveList.addListSelectionListener(event -> {
				PlayTiles playTiles = null;
				for (int i = event.getFirstIndex(); i <= event.getLastIndex(); i++)
				{
					if (this.moveList.isSelectedIndex(i))
					{
						playTiles = this.moveList.getModel().getElementAt(i).getPlayTiles();
						break;
					}
				}
				Playground.this.jGrid.highlightMove(playTiles);
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

								final PlayTiles playTiles = selection.get(0).getPlayTiles();
								play(playTiles);
								Playground.this.commandPrompt.setText("");

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
					showPossibilitiesButton.setText(LABEL_DISPLAY);
					return;
				}

				final SwingPlayer player = getCurrentSwingPlayer();
				if (player == null)
				{
					showMessage(MESSAGES.getString("player.not.at.turn"));
					return;
				}

				final Set<PlayTiles> playTiles = this.bruteForceMethod.getLegalMoves(getGrid(),
						Playground.this.game.getRack(player, player.getPlayerKey()));
				this.legalMoves = new ArrayList<>();

				for (final PlayTiles playTile : playTiles)
				{
					this.legalMoves.add(getGrid().getMetaInformation(playTile));
				}

				possibleMovePanel.add(
						new JScrollPane(this.moveList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED)
				);

				final JPanel orderMethodPanel = new JPanel();
				possibleMovePanel.add(orderMethodPanel, BorderLayout.NORTH);
				orderMethodPanel.add(new OrderButton(MESSAGES.getString("score"), Grid.MoveMetaInformation.SCORE_COMPARATOR));
				orderMethodPanel.add(new OrderButton(MESSAGES.getString("length"), Grid.MoveMetaInformation.WORD_LENGTH_COMPARATOR));
				this.orderButGroup.getElements().asIterator().next().doClick();
				possibleMovePanel.validate();
			}
			catch (ScrabbleException e1)
			{
				e1.printStackTrace();
			}

			showPossibilitiesButton.setText(LABEL_HIDE);
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

	/**
	 * Play the move: inform the server about it and clear the client input field.
	 *
	 * @param playTiles move to play
	 */
	private void play(final PlayTiles playTiles) throws ScrabbleException.NotInTurn, ScrabbleException.InvalidSecretException
	{
		final SwingPlayer player = getCurrentSwingPlayer();
		assert player != null;
		this.game.play(player.getPlayerKey(), this.currentPlay, playTiles);
	}

	/**
	 * @return the current player or {@code null} when current not Swing one
	 */
	private SwingPlayer getCurrentSwingPlayer()
	{
		if (!(this.currentPlay.player instanceof SwingPlayer))
		{
			return null;
		}
		return (SwingPlayer) this.currentPlay.player;
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
