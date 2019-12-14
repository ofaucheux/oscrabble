package oscrabble.server;

import oscrabble.player.AbstractPlayer;

import java.util.UUID;

public class Play
{
	public final UUID uuid = UUID.randomUUID();

	/**
	 * Executed action. Will be filled by the server after confirmation of the move.
	 */
	public Action action;

	public final AbstractPlayer player;

	public int score; // TODO: to be filled.

	public Play(final AbstractPlayer player)
	{
		this.player = player;
	}

	/**
	 *
	 * @return true if the play already have been played.
	 */
	public boolean isDone()
	{
		return this.action != null;
	}
}
