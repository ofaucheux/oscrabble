package oscrabble.client;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.ScrabbleException;
import oscrabble.client.ui.CursorImage;
import oscrabble.client.ui.JScoreboard;
import oscrabble.client.ui.ServerConfigPanel;
import oscrabble.client.utils.I18N;
import oscrabble.client.utils.StateUtils;
import oscrabble.controller.Action;
import oscrabble.controller.Action.PlayTiles;
import oscrabble.data.GameState;
import oscrabble.data.Player;
import oscrabble.data.ScrabbleRules;
import oscrabble.data.objects.Coordinate;
import oscrabble.data.objects.Grid;
import oscrabble.exception.IllegalCoordinate;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.IOError;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Playground for Swing: grid and other components which are the same for all players.
 */
class Playground {

	static final Font MONOSPACED;

	/**
	 * Image following the mouse cursor
	 */
	private final CursorImage cursorImage;

	/**
	 * Filter, das alles Eingetragene Uppercase schreibt
	 */
	private final static DocumentFilter UPPER_CASE_DOCUMENT_FILTER = new DocumentFilter() {
		public void insertString(DocumentFilter.FilterBypass fb, int offset,
								 String text, AttributeSet attr
		) throws BadLocationException {

			fb.insertString(offset, toUpperCase(text), attr);
		}

		public void replace(DocumentFilter.FilterBypass fb, int offset, int length,
							String text, AttributeSet attrs
		) throws BadLocationException {

			fb.replace(offset, length, toUpperCase(text), attrs);
		}

		/**
		 * Entfernt die Umlaute und liefert alles Uppercase.
		 * TODO: für Frz. sinnvoll, für Deutsch aber sicherlich nicht..
		 */
		private String toUpperCase(String text) {
			text = Normalizer.normalize(text, Normalizer.Form.NFD);
			text = text.replaceAll("[^\\p{ASCII}]", ""); //NON-NLS
			text = text.replaceAll("\\p{M}", ""); //NON-NLS
			return text.toUpperCase();
		}
	};

	private static final Logger LOGGER = LoggerFactory.getLogger(Playground.class);

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

	/**
	 * Listing of the history of the game
	 */
	private final JList<oscrabble.data.Action> historyList;

	/**
	 * The frame containing the grid (and other things)
	 */
	JFrame gridFrame;

	/**
	 * The client this playground is for
	 */
	final Client client;

	/**
	 * Action defined in the command prompt
	 */
	Action action;

	private final PossibleMoveDisplayer pmd;

	static {
		try (InputStream resource = Playground.class.getResourceAsStream("nk57-monospace-cd-bk.ttf")) { //NON-NLS
			assert resource != null;
			MONOSPACED = Font.createFont(Font.TRUETYPE_FONT, resource).deriveFont(14f);
		} catch (final Exception exception) {
			throw new IOError(exception);
		}
	}

	Playground(final Client client) {
		this.client = client;
		this.jGrid = new JGrid();
		this.jGrid.setPlayground(this);
		this.jScoreboard = new JScoreboard();
		this.jScoreboard.setFocusable(false);
		this.commandPrompt = new JTextField();
		final CommandPromptAction promptListener = new CommandPromptAction();
		this.commandPrompt.addActionListener(promptListener);
		this.commandPrompt.setFont(this.commandPrompt.getFont().deriveFont(20f));
		final AbstractDocument document = (AbstractDocument) this.commandPrompt.getDocument();
		document.addDocumentListener(promptListener);
		document.setDocumentFilter(UPPER_CASE_DOCUMENT_FILTER);

		this.gridFrame = new JFrame();
		final WindowAdapter frameAdapter = new WindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent e) {
				final int confirm = JOptionPane.showConfirmDialog(Playground.this.gridFrame, I18N.get("quit.the.game"), I18N.get("confirm.quit"), JOptionPane.YES_NO_OPTION);
				if (confirm == JOptionPane.YES_OPTION) {
					if (client != null) {
						Playground.this.client.quitGame();
					} else {
						Playground.this.dispose();
					}
				}
			}
		};
		this.gridFrame.setFocusTraversalPolicyProvider(true);
		this.gridFrame.setFocusTraversalPolicy(new SingleComponentFocusTransversalPolicy(this.commandPrompt));
		this.gridFrame.addWindowListener(frameAdapter);
		this.gridFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		this.gridFrame.setLayout(new BorderLayout());
		this.cursorImage = new CursorImage("", Color.WHITE);
		this.cursorImage.setOnWindow(this.gridFrame);

		final JPanel mainPanel_01 = new JPanel();
		mainPanel_01.setLayout(new BoxLayout(mainPanel_01, BoxLayout.PAGE_AXIS));
		mainPanel_01.add(this.jGrid);
		mainPanel_01.add(this.commandPrompt);
		this.gridFrame.add(mainPanel_01);

		final JPanel eastPanel = new JPanel(new BorderLayout());
		final JPanel panel1 = new JPanel();
		panel1.setPreferredSize(this.jScoreboard.getPreferredSize());
		panel1.setLayout(new BoxLayout(panel1, BoxLayout.PAGE_AXIS));
		panel1.add(this.jScoreboard);
		panel1.add(Box.createVerticalGlue());

		if (client == null) {
			panel1.add(new JTextField("Place for PossibleMoveDisplayer")); //NON-NLS
			this.pmd = null;
		} else {
			this.pmd = new PossibleMoveDisplayer(this.client.getDictionary());
			this.pmd.addSelectionListener(l -> this.jGrid.highlightPreparedAction((PlayTiles) l, getRules()));
			this.pmd.setFont(MONOSPACED);
			this.pmd.getMoveList().addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(final MouseEvent e) {
					if (e.getClickCount() == 2) {
						client.executeCommand(Playground.this.pmd.getSelectedAction());
					}
				}
			});
			this.client.listeners.add(() -> this.pmd.reset());
			panel1.add(this.pmd.mainPanel);
		}

		final JPanel historyPanel = new JPanel(new BorderLayout());
		historyPanel.setBorder(new TitledBorder(I18N.get("moves")));
		this.historyList = new JList<>();
		this.historyList.setFont(MONOSPACED);
		this.historyList.setFocusable(false);
		this.historyList.setCellRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
				final oscrabble.data.Action action = (oscrabble.data.Action) value;
				super.getListCellRendererComponent(list, action, index, isSelected, cellHasFocus);
				setText(String.format("%s %s pts", //NON-NLS
						StringUtils.rightPad(action.notation, 13),
						StringUtils.leftPad(Integer.toString(action.getScore()), 3))
				);
				return this;
			}
		});
		final JScrollPane scrollPane = new JScrollPane(this.historyList);
		historyPanel.add(scrollPane);
		this.historyList.addPropertyChangeListener("model", (e) -> {   // scroll at end by content change
					SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(Integer.MAX_VALUE));
				}
		);

		final JPopupMenu popup = new JPopupMenu();
		if (client != null) {
			DisplayDefinitionAction dda = new DisplayDefinitionAction(client.getDictionary(), () -> {
				PlayTiles action = getSelectedHistoryAction();
				return action == null ? null : Collections.singleton(action.word);
			});
			final Object waitToken = new Object();
			dda.beforeActionListeners.add(() -> client.addWaitToken(waitToken, false));
			dda.afterActionListeners.add(() -> client.addWaitToken(waitToken, true));

			dda.setRelativeComponentPosition(this.gridFrame);
			popup.add(dda);
		}
		this.historyList.setComponentPopupMenu(popup);

		this.historyList.addListSelectionListener(event -> {
			if (event.getValueIsAdjusting()) {
				return;
			}
			final PlayTiles action = getSelectedHistoryAction();
			this.jGrid.highlightSquares(action);
		});

		panel1.add(historyPanel);
		// todo: rollback
		final JButton rollbackButton = new JButton(new AbstractAction(I18N.get("rollback")) {
			@Override
			public void actionPerformed(final ActionEvent e) {
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

		// todo: config panel
		final JButton serverConfigButton = new JButton(new AbstractAction(I18N.get("server.configuration")) {
			@Override
			public void actionPerformed(final ActionEvent e) {
				new ServerConfigPanel(client.server);
			}
		});
		panel1.add(serverConfigButton);
		eastPanel.add(panel1, BorderLayout.CENTER);
		final JLabel versionLabel = new JLabel();
		versionLabel.setFont(versionLabel.getFont().deriveFont(9f));
		versionLabel.setText(Application.getFormattedVersion());
		panel1.add(versionLabel);
		this.gridFrame.add(eastPanel, BorderLayout.LINE_END);

		this.gridFrame.pack();
		this.gridFrame.setResizable(false);
		this.gridFrame.setVisible(true);

		this.commandPrompt.requestFocus();
	}



	/**
	 * Dispose the UI
	 */
	void dispose() {
		this.gridFrame.dispose();
	}

	/**
	 * @return selected action in the history list. {@code null} if nothing is selected or if the selected item is not a {@link PlayTiles}.
	 */
	private PlayTiles getSelectedHistoryAction() {
		final Action action;
		final int index = this.historyList.getSelectedIndex();
		if (index == -1) {
			return null;
		}
		try {
			action = Action.parse(this.historyList.getModel().getElementAt(index));
		} catch (ScrabbleException.NotParsableException e) {
			throw new Error(e);
		}
		return action instanceof PlayTiles ? ((PlayTiles) action) : null;
	}

	/**
	 * Update the UI to reflect the changes in the game.
	 * <li>update scoreboard and history list
	 * <li>display some information if required, p.ex. « end of game »
	 *
	 * @param state
	 * @param rack
	 */
	public void refreshUI(final GameState state, final List<Character> rack) {
		this.jGrid.setGrid(state.getGrid());
		this.jScoreboard.updateDisplay(state.players, state.playerOnTurn);
		this.historyList.setListData(state.playedActions.toArray(new oscrabble.data.Action[0]));
		if (this.pmd != null) {
			this.pmd.refresh(client.server, state, rack);
		}

		final Player onTurn = StateUtils.getPlayerOnTurn(state);

		if (state.state == GameState.State.STARTED
				&& onTurn != null
				&& !this.client.player.equals(onTurn.id)
		) {
			this.cursorImage.setText(I18N.get("0.on.turn", onTurn.name));
		} else {
			this.cursorImage.setText(null);
		}

		if (state.state == GameState.State.ENDED) {
			final JLabel gameOverLabel = new JLabel(I18N.get("game.over")); //todo: i18n
			gameOverLabel.setFont(gameOverLabel.getFont().deriveFont(60f).deriveFont(Font.BOLD));
			gameOverLabel.setForeground(Color.ORANGE);
			gameOverLabel.setHorizontalAlignment(JTextField.CENTER);
			gameOverLabel.setOpaque(false);
			final JPanel glassPane = new JPanel();
			this.gridFrame.setGlassPane(glassPane);
			glassPane.setLayout(new BorderLayout());
			glassPane.add(gameOverLabel);
			glassPane.setOpaque(false);
			glassPane.setVisible(true);
			this.gridFrame.pack();
		}
	}

	public String getCommand() {
		return this.commandPrompt.getText();
	}

	public void setScoreboardPlayerAdditionalComponent(final Map<UUID, JComponent> components) {
		components.forEach(
				(p, c) -> this.jScoreboard.setAdditionalComponent(p, c)
		);
	}

	private oscrabble.controller.Action getPreparedMove() throws ScrabbleException.NotParsableException {

//		assert this.client != null;
//
		// TODO
//			final SwingPlayer player = getCurrentSwingPlayer();
//			if (player == null)
//			{
//				throw new IllegalStateException("Player is not current one");
//			}

		String command = Playground.this.getCommand();
		final StringBuilder sb = new StringBuilder();

		boolean joker = false;
		for (final char c : command.toCharArray()) {
			if (c == '*') {
				joker = true;
			} else {
				sb.append(joker ? Character.toLowerCase(c) : c);
				joker = false;
			}
		}

		Playground.this.action = null;

		final Pattern playCommandPattern = Pattern.compile("(?:play\\s+)?(.*)", Pattern.CASE_INSENSITIVE);
		Matcher matcher;
		if ((matcher = playCommandPattern.matcher(sb.toString())).matches()) {
			final StringBuilder inputWord = new StringBuilder(matcher.group(1));
			if (inputWord.toString().trim().isEmpty()) {
				this.action = null;
			} else {
				this.action = oscrabble.controller.Action.parse(
						getPlayer(),
						inputWord.toString()
				);
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
//								throw new JokerPlacementException(Locale.get("no.enough.jokers"), null);
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
//										Locale.get("cannot.place.the.jokers.several.emplacement.possible.use.the.a.notation"),
//										null);
//							}
//						}
//					}
//				}
			this.action = Action.parse(getPlayer(), inputWord.toString());
			LOGGER.debug("Word after having positioned white tiles: " + inputWord); //NON-NLS
		} else {
			this.action = null;
		}
		return this.action;
	}

	/**
	 *
	 * @return
	 */
	private UUID getPlayer() {
		return this.client == null ? null : this.client.player;
	}

	/**
	 * Set a cell as the start of the future tipped word.
	 */
	void setStartCell(final JGrid.JSquare click) {
		String newPrompt = null;
		try {
			final String currentPrompt = this.getCommand();
			if (currentPrompt.isEmpty()) {
				newPrompt = click.square.getNotation(Grid.Direction.HORIZONTAL) + " ";
			} else {
				final Pattern pattern = Pattern.compile("(\\S*)(\\s+(\\S*))?");
				final Matcher m;
				if ((m = pattern.matcher(currentPrompt)).matches()) {
					final Coordinate currentCoordinate = Coordinate.parse(m.group(1));
					final String word = m.group(3);
					final Coordinate clickedCoordinate = Coordinate.parse(click.square.getCoordinate());
					clickedCoordinate.direction =
							clickedCoordinate.sameCell(currentCoordinate)
									? currentCoordinate.direction.other()
									: currentCoordinate.direction;

					newPrompt = clickedCoordinate.getNotation() + " ";
					if (word != null && !word.trim().isEmpty()) {
						newPrompt += word;
					}
				}
			}

		} catch (final IllegalCoordinate e) {
			// OK: noch kein Prompt vorhanden, oder nicht parsable.
		}

		if (newPrompt != null) {
			this.commandPrompt.setText(newPrompt);
		}
	}

	/**
	 * Execute the command contained in the command prompt
	 */
	void executeCommand() {
		if (this.client == null) {
			JOptionPane.showMessageDialog(Playground.this.gridFrame, I18N.get("this.playground.has.no.client"));
			return;
		}

		SwingUtilities.invokeLater(() ->
		{
			this.client.executeCommand(getCommand());
			this.commandPrompt.setText("");
		});
	}

	/**
	 *
	 * @throws ScrabbleException.NotParsableException
	 */
	private void displayPreparedMove() throws ScrabbleException.NotParsableException {
		final Action action = getPreparedMove();
		if (!(action instanceof PlayTiles)) {
			return;
		}

		final PlayTiles playAction = ((PlayTiles) action);
		this.jGrid.positionArrow(playAction);
		this.jGrid.highlightPreparedAction(
				playAction,
				getRules()
		);
	}

	/**
	 *
	 * @return
	 */
	private ScrabbleRules getRules() {
		return this.client == null ? null : this.client.scrabbleRules;
	}

	private class CommandPromptAction extends AbstractAction implements DocumentListener {

		@Override
		public void actionPerformed(final ActionEvent e) {
			SwingUtilities.invokeLater(() -> executeCommand());
		}

		@Override
		public void insertUpdate(final DocumentEvent e) {
			changedUpdate(e);
		}

		@Override
		public void removeUpdate(final DocumentEvent e) {
			changedUpdate(e);
		}

		@Override
		public void changedUpdate(final DocumentEvent e) {
//			if (Playground.this.client == null) {
//				return;
//			}

			try {
				displayPreparedMove();
			} catch (final ScrabbleException ignored) {
			}

			Playground.this.jGrid.repaint();
		}
	}
}
