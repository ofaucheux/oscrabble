package oscrabble.client;

import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.ScrabbleException;
import oscrabble.client.utils.I18N;
import oscrabble.client.ui.AIPlayerConfigPanel;
import oscrabble.controller.ScrabbleServerInterface;
import oscrabble.data.*;
import oscrabble.player.ai.AIPlayer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

public class Client {

	public static final Logger LOGGER = LoggerFactory.getLogger(Client.class);

	/**
	 * Used dictionary
	 */
	public final IDictionary dictionary;

	final UUID game;
	final UUID player;
	final private Set<Listener> listeners = new HashSet<>();

	final Playground playground;
	private final JRack rack;
	final ScrabbleServerInterface server;

	/**
	 * Listeners to be run on game leaving
	 */
	private final HashSet<Runnable> onQuitGame = new HashSet<>();

	private GameState lastKnownState = null; // TODO: use it in server.play() too

	/**
	 * Rules of the game
	 */
	final ScrabbleRules scrabbleRules;

	/**
	 * last played turn
	 */
	private UUID lastPlayedTurn;

	public Client(final ScrabbleServerInterface server, final IDictionary dictionary, final UUID game, final UUID player) throws ScrabbleException {
		this.server = server;
		this.dictionary = dictionary;
		this.game = game;
		this.player = player;
		this.scrabbleRules = this.server.getRules(this.game);

		prepareCommands();

		this.playground = new Playground(this);
		this.onQuitGame.add(this.playground::dispose);
		this.rack = new JRack();
	}

	private final Set<Object> waitingTokens = new HashSet<>();
	private final Object waitingAfterOtherPlayToken = new Object();

	public void addListener(Listener listener) {
		this.listeners.add(listener);
	}

	/**
	 * Display the components
	 */
	void displayAll() {
		final JFrame gridFrame = this.playground.gridFrame;
		gridFrame.setVisible(true);
		final JFrame rackFrame = new JFrame();
		rackFrame.add(this.rack);
		rackFrame.setAutoRequestFocus(false);
		rackFrame.setVisible(true);
		rackFrame.setLocation(
				gridFrame.getX() + gridFrame.getWidth(),
				gridFrame.getY() + (gridFrame.getHeight() / 2) - (this.rack.getHeight() / 2)
		);
		rackFrame.setVisible(true);
		rackFrame.pack();
		this.onQuitGame.add(rackFrame::dispose);

		this.playground.gridFrame.addWindowFocusListener(new WindowAdapter() {
			@Override
			public void windowGainedFocus(final WindowEvent e) {
				rackFrame.toFront();
			}
		});
		rackFrame.addWindowFocusListener(new WindowAdapter() {
			@Override
			public void windowGainedFocus(final WindowEvent e) {
				Client.this.playground.gridFrame.toFront();
			}
		});

		final GameStateDispatcherThread th = new GameStateDispatcherThread();
		th.setName("State DTh"); //NON-NLS
		th.setDaemon(true);
		th.start();
	}

	public boolean isVisible() {
		return this.playground.gridFrame.isVisible();
	}

	@SneakyThrows
	private void refreshUI(final GameState state)   // todo: display for several racks
	{
		LOGGER.info("Refresh ui called with turn id " + state.turnId); //NON-NLS
		final Bag rack = this.server.getRack(this.game, this.player);
		this.lastKnownState = state;
		this.playground.refreshUI(state, rack.getChars());
		this.rack.setTiles(rack.tiles);
		if (!state.playedActions.isEmpty()) {
			final UUID lastPlayedTurn = state.playedActions.get(state.playedActions.size() - 1).getTurnId();
			if (Client.this.lastPlayedTurn != lastPlayedTurn) {
				Client.this.lastPlayedTurn = lastPlayedTurn;
				final ScheduledFuture<Void> blinkAction = this.playground.jGrid.scheduleLastWordBlink(lastPlayedTurn);
				blinkAction.get();
			}
		}
		addWaitToken(this.waitingAfterOtherPlayToken, this.player.equals(state.getPlayerOnTurn()));
	}

	/**
	 * Add or remove an token informing the application a waiting action is running.
	 *
	 * @param token  the token
	 * @param remove is the token to be removed instead of being added?
	 */
	void addWaitToken(final Object token, final boolean remove) {
		if (remove) {
			this.waitingTokens.remove(token);
		} else {
			this.waitingTokens.add(token);
		}

		final Cursor cursor = this.waitingTokens.isEmpty()
				? Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
				: Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
		this.playground.gridFrame.setCursor(cursor);
	}

	private void prepareCommands() {
		//			todo ?this.commands.put(KEYWORD_HELP, new Command("display help", (args -> {
//						final StringBuffer sb = new StringBuffer();
//						sb.append("<table border=1>");
//						CommandPromptAction.this.commands.forEach(
//								(k, c) -> sb.append("<tr><td>").append(k).append("</td><td>").append(c.description).append("</td></tr>"));
//						sb.setLength(sb.length() - 1);
//						sb.append("</table>");
//						telnetFrame.appendConsoleText("blue", sb.toString(), false);
//						return null;
//					}))
//			);

// todo?
//			this.commands.put("isValid", new Command("check if a word is valid", (args -> {
//				final String word = args[0];
//				final Collection<String> mutations = Playground.this.game.getDictionary().getMutations(
//						word.toUpperCase());
//				final boolean isValid = mutations != null && !mutations.isEmpty();
//				telnetFrame.appendConsoleText(
//						isValid ? "blue" : "red",
//						word + (isValid ? (" is valid " + mutations) : " is not valid"),
//						true);
//				return null;
//			})));
	}

	/**
	 * Execute a command
	 */
	public void executeCommand(final String command) {
		if (command == null) {
			return;
		}
//		todo if (command.startsWith("/"))
//		{
//			final String[] splits = command.split("\\s+");
//			String keyword = splits[0].substring(1).toLowerCase();
//			if (!this.commands.containsKey(keyword))
//			{
//				keyword = KEYWORD_HELP;
//			}
//			CommandPromptAction.Command c = this.commands.get(keyword);
//			telnetFrame.appendConsoleText("black", "> " + command, false);
//			c.action.apply(Arrays.copyOfRange(splits, 1, splits.length));
//			return;
//		}

		try {
//				final SwingPlayer swingPlayer = getCurrentSwingPlayer();
//				if (swingPlayer != null && swingPlayer == Playground.this.currentPlay.player)
//				{
//			final Matcher m;
			// Todo
//					if ((m = PATTERN_EXCHANGE_COMMAND.matcher(command)).matches())
//					{
//						Playground.this.game.play(swingPlayer.getPlayerKey(), Playground.this.currentPlay, new Exchange(m.group(1)));
//					}
//					else if (PATTERN_PASS_COMMAND.matcher(command).matches())
//					{
//						Playground.this.game.play(swingPlayer.getPlayerKey(), Playground.this.currentPlay, SkipTurn.SINGLETON);
//					}
//					else
			final oscrabble.data.Action action = oscrabble.data.Action.builder()
					.player(this.player)
					.turnId(UUID.randomUUID()) //TODO: the game should give the id
					.notation(command)
					.build();
			final PlayActionResponse response = this.server.play(this.game, action);
			treatNewState(response.gameState);
			if (!response.success) {
				// todo: i18n
				final StringBuilder sb = new StringBuilder("<html>").append(I18N.get("play.refused")).append(response.message);
				if (response.retryAccepted) {
					sb.append("<br>").append(I18N.get("retry.accepted"));
				}
				throw new ScrabbleException(sb.toString());
			}
		} catch (ScrabbleException | InterruptedException ex) {
			JOptionPane.showMessageDialog(this.playground.gridFrame, ex.getMessage());
		}
	}

	public IDictionary getDictionary() {
		return this.dictionary;
	}

	/**
	 * Quit the game
	 */
	void quitGame() {
		// TODO: schickt "ende" dem Server
		this.onQuitGame.forEach(Runnable::run);
	}

	void treatNewState(final GameState state) throws ScrabbleException {
		refreshUI(state);
		this.listeners.forEach(l -> l.onNewState());
		this.server.acknowledgeState(this.game, this.player, state);
	}

	/**
	 * Set the list of ai players
	 * @param aiPlayers
	 */
	public void setAIPlayers(final HashSet<AIPlayer> aiPlayers) {
		final HashMap<UUID, JComponent> additionalPlayerComponents = new HashMap<>();
		for (final AIPlayer ai : aiPlayers) {
			final JComponent configButton = new JButton(new AbstractAction("...") {
				@Override
				public void actionPerformed(final ActionEvent e) {
					JOptionPane.showMessageDialog(
							null,
							new AIPlayerConfigPanel(ai),
							MessageFormat.format(I18N.get("properties.for.0"), ai.name),
							JOptionPane.PLAIN_MESSAGE
					);
				}
			});
			configButton.setPreferredSize(new Dimension(16, 16));
			configButton.setFocusable(false);
			configButton.setBorder(new EmptyBorder(2,2,2,2));
			additionalPlayerComponents.put(ai.uuid, configButton);
		}
		this.playground.setScoreboardPlayerAdditionalComponent(additionalPlayerComponents);
	}

//	/**
//	 * Display a message. It will not be displayed if the same message has been displayed some seconds ago and no other one since.
//	 *
//	 * @param message message to display
//	 */
//	void showMessage(final Object message)
//	{
//		if (this.lastMessage != null
//				&& this.lastMessage.message.equals(message)
//				&& System.currentTimeMillis() < this.lastMessage.displayTime + 5000)
//		{
//			return;
//		}
//
//		this.lastMessage = new Playground.DisplayedMessage();
//		this.lastMessage.message = message;
//		this.lastMessage.displayTime = System.currentTimeMillis();
//
//		final KeyboardFocusManager previous = KeyboardFocusManager.getCurrentKeyboardFocusManager();
//		KeyboardFocusManager.setCurrentKeyboardFocusManager(new DefaultFocusManager());
//		try
//		{
//			JOptionPane.showMessageDialog(null, message);
//		}
//		finally
//		{
//			KeyboardFocusManager.setCurrentKeyboardFocusManager(previous);
//		}
//
//	}

	public interface Listener {
		void onNewState();
	}

	/**
	 * Thread to update the display of the state of the game
	 */
	private class GameStateDispatcherThread extends Thread {
		@Override
		public void run() {
			GameState state;
			while (true) {
				try {

					state = Client.this.server.getState(Client.this.game);
					if (!state.equals(Client.this.lastKnownState)) {
						treatNewState(state);
					}
					if (state.state == GameState.State.ENDED) {
						break;
					}
					//noinspection BusyWait
					Thread.sleep(100);

				} catch (InterruptedException | ScrabbleException e) {
					LOGGER.error("Error " + e, e); //NON-NLS
					JOptionPane.showMessageDialog(Client.this.playground.gridFrame, e.toString());
				}
			}

			// Display end of game message
			Client.this.playground.gridFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			final List<Player> players = new ArrayList<>(state.getPlayers());
			players.sort(Comparator.comparingInt(Player::getScore).reversed());
			final int bestScore = players.get(0).score;
			final String delimiter = " " + I18N.get("and") + " ";
			final Set<String> winners = players.stream()
					.filter(p -> p.score == bestScore)
					.map(Player::getName)
					.collect(Collectors.toSet());

			final StringBuilder sb = new StringBuilder();
			sb.append("<html>")
					.append(I18N.get("end.of.game"))
					.append("<br>")
					.append(
							winners.size() == 1
									? I18N.get("winner.is")
									: I18N.get("winners.are"))
					.append(" ")
					.append(String.join(
							delimiter,
							winners));

			JOptionPane.showMessageDialog(Client.this.playground.gridFrame, sb.toString());

			LOGGER.debug("Thread ends"); //NON-NLS
		}
	}
}
