package oscrabble.controller;

import oscrabble.ScrabbleException;
import oscrabble.data.*;
import oscrabble.data.Action;

import java.util.Collection;
import java.util.UUID;

public interface ScrabbleServerInterface {

	/**
	 * Compute the score of actions. No check against dictionary occurs. The order of the result is the same as the one of the parameter.
	 *
	 * @param game
	 * @param notations
	 * @return
	 * @throws ScrabbleException.CommunicationException
	 */
	Collection<Score> getScores(final UUID game, final Collection<String> notations) throws ScrabbleException.CommunicationException;

	/**
	 * @return state of the game
	 * @throws ScrabbleException.CommunicationException
	 */
	GameState getState(final UUID game) throws ScrabbleException.CommunicationException;

	void acknowledgeState(final UUID game, final UUID player, final GameState state);

	/**
	 * Get the bag of a player.
	 *
	 * @param game
	 * @param player
	 * @return
	 */
	Bag getRack(final UUID game, final UUID player /* todo: secret */);

	/**
	 * Play an action.
	 */
	PlayActionResponse play(final UUID game, final Action buildAction) throws ScrabbleException;

	/**
	 * Create a game
	 *
	 * @return id of the created new game
	 */
	UUID newGame();

	/**
	 * Add a player
	 *
	 * @param name of the player
	 */
	UUID addPlayer(final UUID game, final String name);


	/**
	 * Start the game.
	 */
	void startGame(final UUID game);

	/**
	 * Get the rules of a game.
	 *
	 * @param game
	 * @return
	 */
	ScrabbleRules getRules(final UUID game);

	/**
	 * Attach or detach a player.
	 *
	 * @param game
	 * @param player
	 * @param attach if false, a detach occurs.
	 */
	void attach(final UUID game, final UUID player, final boolean attach);

}
