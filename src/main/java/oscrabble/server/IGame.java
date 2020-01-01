package oscrabble.server;

import oscrabble.Grid;
import oscrabble.Rack;
import oscrabble.ScrabbleException;
import oscrabble.configuration.Configuration;
import oscrabble.dictionary.Dictionary;
import oscrabble.player.AbstractPlayer;

import java.util.List;
import java.util.UUID;

public interface IGame
{
	void setState(Game.State state);

	/**
	 * Registriert einen neuen Spieler.
	 * @param player der Spieler
	 */
	void addPlayer(AbstractPlayer player);

	/**
	 * Register a listener.
	 * @param listener listener to register
	 */
	void addListener(Game.GameListener listener);

	/**
	 * Play an action. The player must call this function to inform the server of the action he plays.
	 * @param clientKey key of the client
	 * @param play references to the play
	 * @param action action to be done
	 * @return score score of this play
	 */
	int play(UUID clientKey, Play play, Action action) throws ScrabbleException.NotInTurn, ScrabbleException.InvalidSecretException;

	/**
	 *
	 * @return den Spiel, der am Ball ist.
	 */
	AbstractPlayer getPlayerToPlay();

	List<IPlayerInfo> getPlayers();

	/**
	 * @return the history of the game.
	 */
	Iterable<Game.HistoryEntry> getHistory();

	Dictionary getDictionary();

	Grid getGrid();

	/**
	 * @return a copy of the rack of the player
	 */
	Rack getRack(AbstractPlayer player, UUID clientKey) throws ScrabbleException;

	int getScore(final AbstractPlayer player);

	/**
	 * Editiere die Parameters eines Spielers.
	 *
	 * @param caller UUID des Players, der die Funktion aufruft
	 * @param player anderer Player
	 */
	void editParameters(UUID caller, IPlayerInfo player);

	/**
	 * Send a message to all players.
	 *
	 * @param sender sender
	 * @param message message
	 */
	void sendMessage(AbstractPlayer sender, String message);

	/**
	 * Inform the server that a player leaves the game. This can lead to the end of game, dependently
	 * of the implementation.
	 *
	 * @param player leaving player
	 * @param key key of player, for identification
	 * @param message human readable message to transmit, if any.
	 */
	void quit(AbstractPlayer player, UUID key, String message) throws ScrabbleException;

	/**
	 *
	 * @return the configuration object of the game. Never null.
	 */
	Configuration getConfiguration();

	/**
	 *
	 * @param player Player
	 * @return if the last play of the player was an error. It wasn't an error if a successful retry has token place.
	 * @throws IllegalStateException if the player has never played for the time
	 */
	boolean isLastPlayError(AbstractPlayer player);

	default void rollbackLastMove(AbstractPlayer caller, UUID callerKey) throws ScrabbleException
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * Inform the server about the configuration change of a client.
	 *
	 * @param player the player which configuration has changed
	 * @param playerKey key of the client
	 */
	void playerConfigHasChanged(AbstractPlayer player, UUID playerKey);

	/**
	 * State of the game
	 */
	enum  State
	{BEFORE_START, STARTED, ENDED}
}
