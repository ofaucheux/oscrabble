package oscrabble.player;

import oscrabble.configuration.Configuration;
import oscrabble.server.Server;

import java.util.UUID;

/**
 * Interface for players.
 */
public interface IPlayer extends Server.GameListener {
	/**
	 * @return the configuration object of this player, {@code null} if no such one.
	 */
	Configuration getConfiguration();

	/**
	 * Set the key generated for this client
	 *
	 * @param key the key
	 */
	void setPlayerKey(UUID key);

	/**
	 * @return name of the player
	 */
	String getName();

	/**
	 * @param server set the game the player will play
	 */
	void setGame(Server server);

	/**
	 * @return true if the player doesn't take any active role in the game.
	 */
	boolean isObserver();

	/**
	 * Gibt die Möglichkeit, die Parameters zu ändern. Es kann z.B. durch die Anzeige eines JPanels erfolgen.
	 */
	void editParameters();

	/**
	 * @return key of the player
	 */
	UUID getPlayerKey();

	/**
	 * @return type of the player
	 */
	Server.PlayerType getType();

	/**
	 * @return {@code true} wenn manche Parameters editierbar sind.
	 */
	boolean hasEditableParameters();

	/**
	 * Destroy this player and all its resources
	 */
	default void destroy() {
	}

	;
}
