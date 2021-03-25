package oscrabble.server;

import oscrabble.controller.Action;
import oscrabble.data.Bag;
import oscrabble.data.Player;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Information the server holds about a player.
 */
class PlayerInformation {
	/**
	 * Name
	 */
	final UUID uuid;

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
	 * is a robot?
	 */
	public boolean isRobot;

	/**
	 * Is currently connected with a client
	 */
	boolean isAttached;

	/**
	 * Construct a player from a data object.
	 *
	 * @param dataPlayer data object
	 */
	PlayerInformation(final Player dataPlayer) {
		this.uuid = dataPlayer.id;
		this.name = dataPlayer.name;
		this.score = dataPlayer.score;
		this.isRobot = dataPlayer.isRobot;
	}

	public PlayerInformation(final UUID uuid) {
		if (uuid == null) {
			throw new IllegalArgumentException("Player name not set");
		}
		this.uuid = uuid;
	}

	// TODO
	public oscrabble.configuration.Configuration getConfiguration() {
		return null;
	}

	/**
	 * Construct a data object
	 *
	 * @return the object
	 */
	public oscrabble.data.Player toData() {
		final oscrabble.data.Player data = oscrabble.data.Player.builder()
				.id(this.uuid)
				.name(this.name)
				.score(this.score)
				.isRobot(this.isRobot)
				.isAttached(this.isAttached)
				.build();
		return data;
	}

	@Override
	public String toString() {
		return "Player{" +
				"name='" + this.uuid + '\'' +
				'}';
	}

	public void setName(final String name) {
		this.name = name;
	}
}

