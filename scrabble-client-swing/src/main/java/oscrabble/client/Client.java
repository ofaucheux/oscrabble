package oscrabble.client;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.ScrabbleException;
import oscrabble.controller.MicroServiceScrabbleServer;
import oscrabble.data.GameState;
import oscrabble.data.Player;

import javax.swing.*;
import java.util.UUID;

public class Client
{
	public static final Logger LOGGER = LoggerFactory.getLogger(Client.class);
	private MicroServiceScrabbleServer server;
	private UUID game;
	private UUID player;

	private Playground playground;
	private JRack rack;

	public Client(final MicroServiceScrabbleServer server, final UUID game, final UUID player)
	{
		this.server = server;
		this.game = game;
		this.player = player;

		this.playground = new Playground();
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

	private void refreshUI(final GameState state)
	{
		LOGGER.info("Refresh UI with state " + state.hashCode());
		this.playground.refreshUI(state);

		final Player player = IterableUtils.find(state.getPlayers(), p -> p.id.equals(this.player));
		this.rack.setTiles(player.rack.tiles);
	}

	/**
	 * Thread to update the display of the state of the game
	 */
	private class GameStateDispatcherThread extends Thread
	{
		@Override
		public void run()
		{
			GameState lastKnownState = null;

			while (true)
			{
				try
				{
					Thread.sleep(1000);
					final GameState state = server.getState(game);
					if (!state.equals(lastKnownState))
					{
						refreshUI(state);
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
}
