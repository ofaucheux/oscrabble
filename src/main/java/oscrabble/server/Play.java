package oscrabble.server;

import oscrabble.player.AbstractPlayer;

import java.util.UUID;

public class Play
{
	public final UUID uuid = UUID.randomUUID();

	public final int roundNr;

	public Action action;

	public final AbstractPlayer player;

	public int score; // TODO: to be filled.

	/**
	 * false until the move have been confirmed by the game server.
	 */
	public boolean done;

	public Play(final int roundNr, final AbstractPlayer player)
	{
		this.roundNr = roundNr;
		this.player = player;
	}
}
