package oscrabble.client.utils;

import oscrabble.data.GameState;
import oscrabble.data.Player;

import java.util.UUID;

/**
 * Util functions related to the state of the game.
 */
public class StateUtils {

	/**
	 * @param state state of the game
	 * @return the player on turn
	 */
	public static Player getPlayerOnTurn(GameState state) {
		final UUID uuid = state.getPlayerOnTurn();
		for (final Player player : state.getPlayers()) {
			if (player.id.equals(uuid)) {
				return player;
			}
		}

		return null;
	}
}
