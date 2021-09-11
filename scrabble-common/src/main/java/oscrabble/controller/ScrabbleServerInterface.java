package oscrabble.controller;

import oscrabble.ScrabbleException;
import oscrabble.data.*;
import oscrabble.data.Action;

import java.util.Collection;
import java.util.Set;
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
	Collection<Score> getScores(final UUID game, final Collection<String> notations) throws ScrabbleException;

	/**
	 * @return state of the game
	 * @throws ScrabbleException.CommunicationException
	 */
	GameState getState(final UUID game) throws ScrabbleException;

	/**
	 * Inform the server that the player has read the state.
	 * @param game
	 * @param player
	 * @param state
	 */
	void acknowledgeState(final UUID game, final UUID player, final GameState state) throws ScrabbleException;

	/**
	 * Get the bag of a player.
	 *
	 * @param game
	 * @param player
	 * @return
	 */
	Bag getRack(final UUID game, final UUID player /* todo: secret */) throws ScrabbleException;

	/**
	 * Play an action.
	 */
	PlayActionResponse play(final UUID game, final Action buildAction) throws ScrabbleException, InterruptedException;

	/**
	 * Create a game
	 * TODO: string parameter for the game language
	 *
	 * @return id of the created new game
	 */
	UUID newGame();

	/**
	 * Add a player
	 *
	 * @param name of the player
	 */
	UUID addPlayer(final UUID game, final String name) throws ScrabbleException;


	/**
	 * Start the game.
	 */
	void startGame(final UUID game) throws ScrabbleException;

	/**
	 * Get the rules of a game.
	 *
	 * @param game
	 * @return
	 */
	ScrabbleRules getRules(final UUID game) throws ScrabbleException;

	/**
	 * Attach or detach a player.
	 *
	 * @param game
	 * @param player
	 * @param attach if false, a detach occurs.
	 */
	void attach(final UUID game, final UUID player, final boolean attach) throws ScrabbleException;

	/**
	 * Add a refused word to the current game. The implementation decides if the word will be refused for
	 * future games too.
	 * @param game
	 * @param refusedWord
	 */
	void addRefusedWord(UUID game, String refusedWord);

	/**
	 * @param game
	 * @param refusedWords the list of words the server would refuse.
	 */
	void setRefusedWords(UUID game, Set<String> refusedWords);

	/**
	 * @param game
	 * @return the list of word the server would refuse.
	 */
	Set<String> getAdditionalRefusedWords(UUID game);
}
