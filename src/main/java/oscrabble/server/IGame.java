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
	/**
	 * Registriert einen neuen Spieler.
	 * @param player der Spieler
	 */
	void register(AbstractPlayer player);

	/**
	 * @return score
	 */
	int play(AbstractPlayer player, IAction action);

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

	void markAsIllegal(final String word);

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
}
