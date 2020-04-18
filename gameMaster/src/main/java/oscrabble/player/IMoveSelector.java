package oscrabble.player;

import oscrabble.action.PlayTiles;
import oscrabble.ScrabbleException;

import java.util.Set;

interface IMoveSelector
{
	PlayTiles select(Set<PlayTiles> playTiles) throws ScrabbleException;
}
