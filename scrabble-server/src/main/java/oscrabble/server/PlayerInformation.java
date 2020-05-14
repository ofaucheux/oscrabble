package oscrabble.server;

import oscrabble.controller.Action;
import oscrabble.data.Bag;
import oscrabble.data.Player;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Information the server holds about a player.
 */
class PlayerInformation
{
	/**
	 * Name
	 */
	final UUID uuid;

	/**
	 * Password für die Kommunikation Player &lt;&gt; Server
	 */
	String id;

//		/**
//		 * Queue  to receive events from client
//		 */
//		BlockingQueue<ScrabbleEvent> incomingEventQueue;

	/**
	 * Tiles in the rack, space for a joker.
	 */
	Bag rack = Bag.builder().tiles(new ArrayList<>()).build();

	int score;
	/**
	 * Last played action.
	 */
	Action lastAction;

	/**
	 * Name of the player
	 */
	String name;

	/**
	 * Construct a player from a data object.
	 * @param dataPlayer data object
	 */
	PlayerInformation(final Player dataPlayer)
	{
		this.uuid = dataPlayer.id;
		this.name = dataPlayer.name;
		this.score = dataPlayer.score;
		this.rack = dataPlayer.rack;
	}

	public PlayerInformation(final UUID uuid)
	{
		if (uuid == null)
		{
			throw new IllegalArgumentException("Player name not set");
		}
		this.uuid = uuid;
	}

	// TODO
	public oscrabble.configuration.Configuration getConfiguration()
	{
		return null;
	}

	/**
	 * Construct a data object
	 * @return the object
	 */
	public oscrabble.data.Player toData()
	{
		final oscrabble.data.Player data = oscrabble.data.Player.builder()
				.id(this.uuid)
				.name(this.name)
				.score(this.score)
				.rack(this.rack)
				.build();
		return data;
	}

	@Override
	public String toString()
	{
		return "Player{" +
				"name='" + this.uuid + '\'' +
				'}';
	}

	public void setName(final String name)
	{
		this.name = name;
	}
}

