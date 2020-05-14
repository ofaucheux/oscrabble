package oscrabble.client;

import org.apache.commons.collections4.IterableUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.ScrabbleException;
import oscrabble.controller.MicroServiceScrabbleServer;
import oscrabble.data.Bag;
import oscrabble.data.GameState;
import oscrabble.data.Player;

import javax.swing.*;
import java.util.UUID;
import java.util.regex.Matcher;

public class Client
{
	public static final Logger LOGGER = LoggerFactory.getLogger(Client.class);

	private final MicroServiceScrabbleServer server;
	private final UUID game;
	private final UUID player;

	private final Playground playground;
	private final JRack rack;

	private GameState lastKnownState = null; // TODO: use it in server.play() too

	public Client(final MicroServiceScrabbleServer server, final UUID game, final UUID player)
	{
		this.server = server;
		this.game = game;
		this.player = player;

		prepareCommands();

		this.playground = new Playground(this);
		this.rack = new JRack();
	}

	/**
	 * Display the components
	 */
	void displayAll()
	{
		final JFrame gridFrame = this.playground.gridFrame;
		gridFrame.setVisible(true);
		final JFrame rackFrame = new JFrame();
		rackFrame.add(this.rack);
		rackFrame.setVisible(true);
		rackFrame.setLocation(
				gridFrame.getX() + gridFrame.getWidth(),
				gridFrame.getY() + (gridFrame.getHeight() / 2) - (this.rack.getHeight() / 2)
		);
		rackFrame.setVisible(true);
		rackFrame.pack();

		final GameStateDispatcherThread th = new GameStateDispatcherThread();
		th.setName("State DTh");
		th.setDaemon(true);
		th.start();
	}

	public boolean isVisible()
	{
		return this.playground.gridFrame.isVisible();
	}

	private void refreshUI(final GameState state, final Bag rack)  // todo: display for several racks
	{
		LOGGER.info("Refresh UI with state " + state.hashCode());
		this.playground.refreshUI(state);
		this.rack.setTiles(rack.tiles);
	}

	private void prepareCommands()
	{
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
	 * Thread to update the display of the state of the game
	 */
	private class GameStateDispatcherThread extends Thread
	{
		@Override
		public void run()
		{
			while (true)
			{
				try
				{
					Thread.sleep(1000);
					final GameState state = server.getState(game);
					if (!state.equals(lastKnownState))
					{
						final Bag rack = server.getRack(game, player);
						refreshUI(state, rack);
						lastKnownState = state;
					}
				}
				catch (InterruptedException | ScrabbleException.CommunicationException e)
				{
					LOGGER.error("Error " + e, e);
					JOptionPane.showMessageDialog(Client.this.playground.gridFrame, e.toString());
				}
			}
		}
	}

	/**
	 * Execute a command
	 */
	void executeCommand(final String command)
	{
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

		try
		{
//				final SwingPlayer swingPlayer = getCurrentSwingPlayer();
//				if (swingPlayer != null && swingPlayer == Playground.this.currentPlay.player)
//				{
			final Matcher m;
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
			{
				final oscrabble.data.Action action = oscrabble.data.Action.builder()
						.player(this.player)
						.turnId(UUID.randomUUID()) //TODO: the game should give the id
						.notation(command)
						.build();
				server.play(this.game, action);
				// TODO: reduce the time to waite
			}
		}
		catch (ScrabbleException ex)
		{
			JOptionPane.showMessageDialog(playground.gridFrame, ex.getMessage());
		}
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


}
