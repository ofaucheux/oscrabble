package oscrabble.server;

import org.apache.commons.collections4.Bag;
import oscrabble.*;
import oscrabble.dictionary.Dictionary;
import oscrabble.player.AbstractPlayer;

import java.util.List;
import java.util.UUID;

public interface IScrabbleServer
{
	void register(AbstractPlayer player);

	/**
	 * @return score
	 */
	int play(AbstractPlayer player, IAction action);

	List<IPlayerInfo> getPlayers();

	void markAsIllegal(final String word);

	Dictionary getDictionary();

	Grid getGrid();

	/**
	 * @return a copy of the rack of the player
	 */
	Rack getRack(AbstractPlayer player, UUID clientKey) throws ScrabbleException;

	int getScore(final AbstractPlayer player);
}
