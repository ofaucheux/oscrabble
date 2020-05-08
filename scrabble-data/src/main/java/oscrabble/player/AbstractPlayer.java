package oscrabble.player;

import oscrabble.data.Action;
import oscrabble.data.Player;
import oscrabble.server.IGame;

import java.util.UUID;

/**
 * Abstract class for players
 */
public abstract class AbstractPlayer
{
	/**
	 * UUID of the player
	 */
	public final UUID uuid;

	/**
	 * Name
	 */
	public String name;

	protected AbstractPlayer()
	{
		uuid = UUID.randomUUID();
	}

	/**
	 * Build an action and sign it. (todo)
	 *
	 * @param notation the action in scrabble notation
	 * @return the action
	 */
	public Action buildAction(final String notation)
	{
		final Action action = Action.builder()
				.player(this.uuid)
				.playID(UUID.randomUUID())
				.notation(notation)
				.build();
		return action;
	}

}
