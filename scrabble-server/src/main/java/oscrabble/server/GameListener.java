package oscrabble.server;

import oscrabble.controller.Action;

import java.util.Queue;
import java.util.UUID;


/**
 * A listener
 */
public interface GameListener {
	/**
	 * Sent to all players to indicate who now has to play.
	 */
	default void onPlayRequired(final UUID player) {
	}

	default void onDispatchMessage(String msg) {
	}

	default void afterRollback() {
	}

	/**
	 * @param action ended play
	 */
	default void afterPlay(final Action action) {
	}

	default void beforeGameStart() {
	}

	default void afterGameEnd() {
	}

	Queue<ScrabbleEvent> getIncomingEventQueue();

	/**
	 * Called after the state of the game have changed
	 */
	default void onGameStateChanged() {
	}

	/**
	 * Called after a player has (definitively) play an non admissible play
	 *
	 * @param player player having played the non admissible play
	 * @param action the action which lead to the problem
	 */
	default void afterRejectedAction(final PlayerInformation player, final Action action) {
	}

	/**
	 * Called after the list of the additional refused words has changed
	 */
	void afterAdditionalRefusedWordsChanged();
}

