package oscrabble.player;

import oscrabble.data.Action;

import java.util.UUID;

/**
 * Abstract class for players
 */
public abstract class AbstractPlayer
{
	/**
	 * UUID of the player
	 */
	public UUID uuid;

	/**
	 * Name
	 */
	public String name;

	protected AbstractPlayer(final String name)
	{
		this.name = name;
	}

	/**
	 * Build an action and sign it. (todo)
	 *
	 * @param notation the action in scrabble notation
	 * @return the action
	 */
	public Action buildAction(/*final UUID playID, */final String notation)
	{
		final Action action = Action.builder()
				.player(this.uuid)
//				.playID(playID)
				.playID(UUID.randomUUID())
				.notation(notation)
				.build();
		return action;
	}

}
