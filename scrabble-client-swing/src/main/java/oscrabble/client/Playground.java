package oscrabble.client;

import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.ScrabbleException;
import oscrabble.controller.Action;
import oscrabble.controller.Action.PlayTiles;
import oscrabble.controller.MicroServiceScrabbleServer;
import oscrabble.data.GameState;
import oscrabble.data.objects.Coordinate;
import oscrabble.data.objects.Grid;
import oscrabble.data.objects.Square;
import oscrabble.exception.IllegalCoordinate;
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
import java.awt.geom.AffineTransform;
import java.text.Normalizer;
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

	public static final ResourceBundle MESSAGES = Application.MESSAGES;
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
	private JList<Action> historyList;

	/**
	 * Currently played play.
	 */
	private oscrabble.data.Action currentPlay;
//
//	/**
//	 * Registered Swing players
//	 */
//	private final LinkedList<SwingPlayer> swingPlayers = new LinkedList<>();

	/**
	 * The frame containing the grid (and other things)
	 */
	JFrame gridFrame;

	/**
	 * The client this playground is for
	 */
	private final Client client;

	/**
	 * Action defined in the command prompt
	 */
	private Action action;
	/**
	 * Register a player.
	 *
	 * @param swingPlayer player to register
	 */
//	public void addPlayer(final SwingPlayer swingPlayer)
//	{
//		this.swingPlayers.add(swingPlayer);
//	}

//	public void onPlayRequired(final SwingPlayer caller)
//	{
////		if (!isFirstRegistered(caller))
////		{
////			return;
////		}
//
////		for (final Map.Entry<IPlayerInfo, JScoreboard.ScorePanelLine> entry : this.jScoreboard.scoreLabels.entrySet())
////		{
////			final IPlayerInfo playerInfo = entry.getKey();
////			final JScoreboard.ScorePanelLine line = entry.getValue();
////			line.currentPlaying.setVisible(this.currentPlay != null && playerInfo.getName().equals(this.currentPlay.player.getName()));
////		}
//
//		final Cursor cursor;
//		if (true /* todo this.currentPlay.player instanceof SwingPlayer
//				&& this.swingPlayers.contains(this.currentPlay.player)) */)
//		{
//			// ((SwingPlayer) this.currentPlay.player).updateRack();
//			cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
//		}
//		else
//		{
//			cursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
//		}
//
//		for (final Frame frame : Frame.getFrames())
//		{
//			frame.setCursor(cursor);
//		}
//	}
//
//	/**
//	 * Players which UI has been created.
//	 */
//	private final Set<SwingPlayer> playersWithUI = new HashSet<>();

//	/**
//	 * Inform that the ui of a player has been created.
//	 * @param player player
//	 */
//	public synchronized void afterUiCreated(final SwingPlayer player)
//	{
//		this.playersWithUI.add(player);
//		final int numberSwingPlayers = getNumberSwingPlayers();
//		if (this.playersWithUI.size() == numberSwingPlayers)
//		{
//			final int gap = 150;
//			final int basePosX = this.gridFrame.getX() + this.gridFrame.getWidth();
//			final int basePosY = this.gridFrame.getY() + (this.gridFrame.getHeight() / 2) - ( (gap + this.swingPlayers.get(0).rackFrame.getHeight()) * (numberSwingPlayers - 1) / 2);
//
//			for (int i = 0; i < numberSwingPlayers; i++)
//			{
//				final JDialog rackFrame = this.swingPlayers.get(i).rackFrame;
//				rackFrame.setLocation(
//						basePosX,
//						basePosY + gap * i
//				);
//			}
//		}
//	}
//
//	/**
//	 * @return the number of Swing Players registered for this playground.
//	 */
//	int getNumberSwingPlayers()
//	{
//		return this.swingPlayers.size();
//	}

	private DisplayedMessage lastMessage;

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
		this.telnetFrame = new TelnetFrame("Help");
		this.telnetFrame.frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		this.gridFrame = new JFrame();
		final WindowAdapter frameAdapter = new WindowAdapter()
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
					System.exit(0);
				}
			}
		};
		this.gridFrame.addWindowListener(frameAdapter);
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
				setText(((Action) value).notation);
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
		final JButton rollbackButton = new JButton(new AbstractAction(MESSAGES.getString("rollback"))
		{
			@Override
			public void actionPerformed(final ActionEvent e)
			{
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
			}
		}
		);
		rollbackButton.setEnabled(false);
		panel1.add(rollbackButton);

		this.possibleMovePanel = new JPanel();
		this.possibleMovePanel.setBorder(new TitledBorder(MESSAGES.getString("possible.moves")));
		this.possibleMovePanel.setSize(new Dimension(200, 300));
		this.possibleMovePanel.setLayout(new BorderLayout());
		// todo: AI Empfehlungen
//		final BruteForceMethod bruteForceMethod = new BruteForceMethod(	this.game.getDictionary());
//		showPossibilitiesButton = new JButton(new PossibleMoveDisplayer(this, bruteForceMethod));
//		showPossibilitiesButton.setFocusable(false);
//		resetPossibleMovesPanel();

		panel1.add(this.possibleMovePanel);
		panel1.add(Box.createVerticalGlue());

		// todo: configpanel
		final JPanel configPanel = new JPanel();
//		final ConfigurationPanel configPanel = new ConfigurationPanel(
//				this.game.getConfiguration(),
//				null,
//				Collections.singleton("dictionary")
//		);
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

	private void resetPossibleMovesPanel()
	{
		this.possibleMovePanel.removeAll();
		this.possibleMovePanel.invalidate();
		this.possibleMovePanel.repaint();
		this.showPossibilitiesButton.setText(LABEL_DISPLAY);
		this.possibleMovePanel.add(this.showPossibilitiesButton, BorderLayout.SOUTH);
	}

//	protected synchronized void refreshUI(final SwingPlayer caller)
//	{
//		if (!isFirstRegistered(caller))
//		{
//			return;
//		}
//
//		if (this.flashFuture != null)
//		{
//			this.flashFuture.cancel(true);
//		}
//
//		this.jGrid.repaint();
//		// TODO?
////		this.jScoreboard.refresh();
//
////		TODO
////		final Iterable<GameState> history = this.server.getState(game)
////		this.historyList.setListData(IterableUtils.toList(history).toArray(new HistoryEntry[0]));
//	}

//	/**
//	 * @return {@code true} if the parameter is null or represents the first registered client.
//	 */
//	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
//	private boolean isFirstRegistered(final SwingPlayer caller)
//	{
//		return caller == null || caller == this.swingPlayers.getFirst();
//	}

	/**
	 * Set the game.
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
	 * Execute action after play.
	 *
	 * @param play   the occurred play
	 */
	public void afterPlay(final Action play)
	{
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


	public void refreshUI(final GameState state)
	{
		this.jGrid.setGrid(state.getGrid(), this);
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

	private oscrabble.controller.Action getPreparedMove() throws ScrabbleException.ForbiddenPlayException
	{
		// TODO
//			final SwingPlayer player = getCurrentSwingPlayer();
//			if (player == null)
//			{
//				throw new IllegalStateException("Player is not current one");
//			}

		String command = Playground.this.getCommand();
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

		Playground.this.action = null;

		final Pattern playCommandPattern = Pattern.compile("(?:play\\s+)?(.*)", Pattern.CASE_INSENSITIVE);
		Matcher matcher;
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
			if (inputWord.toString().trim().isEmpty())
			{
				this.action = null;
			}
			else
			{
				this.action = oscrabble.controller.Action.parse(inputWord.toString());
			}
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
			this.action = Action.parse(inputWord.toString());
			LOGGER.debug("Word after having positioned white tiles: " + inputWord);
		}
		else
		{
			this.action = null;
		}
		return this.action;
	}

	/**
	 * Set a cell as the start of the future tipped word.
	 */
	private void setStartCell(final JGrid.JSquare click)
	{
		String newPrompt = null;
		try
		{
			final String currentPrompt = this.getCommand();
			if (currentPrompt.isEmpty())
			{
				newPrompt = click.square.getNotation(Grid.Direction.HORIZONTAL) + " ";
			}
			else
			{
				final Pattern pattern = Pattern.compile("(\\S*)(\\s+(\\S*))?");
				final Matcher m;
				if ((m = pattern.matcher(currentPrompt)).matches())
				{
					final Coordinate currentCoordinate = Coordinate.parse(m.group(1));
					final String word = m.group(3);
					final Coordinate clickedCoordinate = Coordinate.parse(click.square.getCoordinate());
					clickedCoordinate.direction =
							clickedCoordinate.sameCell(currentCoordinate)
									? currentCoordinate.direction.other()
									: currentCoordinate.direction;

					newPrompt = clickedCoordinate.getNotation() + " ";
					if (word != null && !word.trim().isEmpty())
					{
						newPrompt += word;
					}
				}
			}

		}
		catch (final IllegalCoordinate e)
		{
			// OK: noch kein Prompt vorhanden, oder nicht parsable.
		}

		if (newPrompt != null)
		{
			this.commandPrompt.setText(newPrompt);
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
	 * Darstellung der Spielfläche
	 */
	static class JGrid extends JPanel
	{
		final JSquare[][] jSquares;

		private Grid grid;
		private final HashMap<JSquare, MatteBorder> specialBorders = new HashMap<>();

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

		private void highlightWord(final PlayTiles action)
		{
			this.specialBorders.clear();

			final ArrayList<JGrid.JSquare> squares = new ArrayList<>();
			boolean isHorizontal = action.getDirection() == Grid.Direction.HORIZONTAL;
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

			final int INSET = 4;
			final Color preparedMoveColor = Color.RED;

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
			private final AbstractAction showDefinitionAction;
			private final Square square;

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
					tile.addMouseListener(new MouseAdapter()
					{
						@Override
						public void mouseClicked(final MouseEvent e)
						{
							JSquare.this.processMouseEvent(e);
						}
					});
					//noinspection StatementWithEmptyBody
//					if (JGrid.this.hideNewStones && this.square.action == JGrid.this.lastAction)
//					{
//						// don't draw todo: to implement in jtile
//					}
//					else
//					{
//						tile.paintComponent(g2);
//					}
				}
//				else if ((c = preparedMoveStones.get(this.square)) != null)
//				{
//					 TODO
//					JTile.drawTile(g2, this, /* TODO tiles.get(c) */ null, Color.blue);
//				}


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
						JGrid.this.playground.setStartCell(JSquare.this);
					}
				});
			}


			@Override
			protected void paintComponent(final Graphics g)
			{
				super.paintComponent(g);

				final Graphics2D g2 = (Graphics2D) g;

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
					PlayTiles action;
					if (this.playground.action instanceof PlayTiles
							&& (action = ((PlayTiles) this.playground.action)).startSquare.getSquare().equals(this.square))
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

		void setPlayground(final Playground client)
		{
			if (this.playground != null)
			{
				throw new AssertionError("The client is already set");
			}
			this.playground = client;
		}
	}


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

	/**
	 * Execute the command contained in the command prompt
	 */
	void executeCommand()
	{
		if (this.client == null)
		{
			JOptionPane.showMessageDialog(Playground.this.gridFrame, "This playground has no client");
			return;
		}

		Playground.this.client.executeCommand(getCommand());
		this.commandPrompt.setText("");
	}

	private class CommandPromptAction extends AbstractAction implements DocumentListener
	{

		static final String KEYWORD_HELP = "?";
		private Map<String, Command> commands = new LinkedHashMap<>();

		@Override
		public void actionPerformed(final ActionEvent e)
		{
			executeCommand();
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
				final Action action = getPreparedMove();
				if (action instanceof PlayTiles)
				{
					Playground.this.jGrid.highlightWord((PlayTiles) action);
				}
			}
			catch (final ScrabbleException.ForbiddenPlayException e1)
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
