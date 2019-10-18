package oscrabble.player;

import oscrabble.Move;
import oscrabble.ScrabbleException;

import java.util.Set;

interface IMoveSelector
{
	Move select(Set<Move> moves) throws ScrabbleException;
}
