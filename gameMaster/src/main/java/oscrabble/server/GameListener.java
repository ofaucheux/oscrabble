package oscrabble.server;

import oscrabble.server.action.Action;

import java.util.Collection;
import java.util.Queue;


/**
 * A listener
 */
public interface GameListener
{
	/**
	 * Sent to all players to indicate who now has to play.
	 */
	default void onPlayRequired(final Game.Player player) { }

	default void onDispatchMessage(String msg) { }

	default void afterRollback() { }

	/**
	 *
	 * @param action ended play
	 */
	default void afterPlay(final Action action) { }

	default void beforeGameStart() { }

	default void afterGameEnd() { }

	Queue<ScrabbleEvent> getIncomingEventQueue();

	/**
	 * Called after the state of the game have changed
	 */
	default void onGameStateChanged() { }

	/**
	 * Called after a player has (definitively) play an non admissible play
	 * @param player player having played the non admissible play
	 * @param action the action which lead to the problem
	 */
	default void afterRejectedAction(final Game.Player player, final Action action){}

}
