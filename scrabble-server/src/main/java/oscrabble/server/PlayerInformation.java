package oscrabble.server;

import oscrabble.controller.Action;
import oscrabble.data.Bag;

import java.util.ArrayList;

/**
 * Information the server holds about a player.
 */
public class PlayerInformation
{
	/**
	 * Was last play an error?
	 */
	// TODO: remove
	public boolean isLastPlayError;

	/**
	 * Name
	 */
	final String name;

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

	public PlayerInformation(final String name)
	{
		if (name == null)
		{
			throw new IllegalArgumentException("Player name not set");
		}
		this.name = name;
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
				.id(this.id)
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
				"name='" + this.name + '\'' +
				'}';
	}
}
