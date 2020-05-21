package oscrabble.client;

import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import oscrabble.ScrabbleException;
import oscrabble.controller.Action;
import oscrabble.controller.Action.PlayTiles;
import oscrabble.controller.MicroServiceScrabbleServer;
import oscrabble.data.GameState;
import oscrabble.data.objects.Coordinate;
import oscrabble.data.objects.Grid;
import oscrabble.exception.IllegalCoordinate;
import oscrabble.player.AbstractPlayer;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Playground for Swing: grid and other components which are the same for all players.
 */
class Playground
{
	final static int CELL_SIZE = 40;
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
	final JGrid jGrid;

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
	private JList<oscrabble.data.Action> historyList;

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
	@Nullable
	final Client client;

	/**
	 * Action defined in the command prompt
	 */
	Action action;

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
		this.jGrid.setDictionaryComponent(new DictionaryComponent(client.getDictionary()));
		this.jGrid.setPlayground(this);
		this.jScoreboard = new JScoreboard(this);
		this.jScoreboard.setFocusable(false);
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
		this.historyList.setFocusable(false);
		this.historyList.setCellRenderer(new DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus)
			{
				final oscrabble.data.Action action = (oscrabble.data.Action) value;
				super.getListCellRendererComponent(list, action, index, isSelected, cellHasFocus);
				setText(action.notation);
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

		this.commandPrompt.requestFocus();
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

	public void refreshUI(final GameState state)
	{
		this.jGrid.setGrid(state.getGrid(), this);
		this.jScoreboard.updateDisplay(state.players, state.playerOnTurn);
		this.historyList.setListData(state.playedActions.toArray(new oscrabble.data.Action[0]));
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
				this.action = oscrabble.controller.Action.parse(this.client.player, inputWord.toString());
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
			this.action = Action.parse(this.client.player, inputWord.toString());
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
	void setStartCell(final JGrid.JSquare click)
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
			catch (final ScrabbleException e1)
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
