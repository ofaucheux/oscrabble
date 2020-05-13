package oscrabble.client;

import oscrabble.controller.MicroServiceScrabbleServer;
import oscrabble.data.Player;

import javax.swing.*;
import java.util.UUID;

public class Client
{
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
	}

	public boolean isVisible()
	{
		return this.playground.gridFrame.isVisible();
	}
}
