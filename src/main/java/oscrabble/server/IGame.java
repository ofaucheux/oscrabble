package oscrabble.server;

import oscrabble.Grid;
import oscrabble.Rack;
import oscrabble.ScrabbleException;
import oscrabble.dictionary.Dictionary;
import oscrabble.player.AbstractPlayer;
import oscrabble.player.BruteForceMethod;

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
}
