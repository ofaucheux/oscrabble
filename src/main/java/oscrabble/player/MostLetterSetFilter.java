package oscrabble.player;

import oscrabble.Grid;
import oscrabble.Move;
import oscrabble.ScrabbleException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Filter, das nur Wort mit den meisten neuen Buchstaben übrig lässt.
 */
public class MostLetterSetFilter implements IMoveSelector
{
	private final Grid grid;

	public MostLetterSetFilter(final Grid grid)
	{
		this.grid  = grid;
	}

	@Override
	public Move select(final Set<Move> moves) throws ScrabbleException
	{
		final List<Move> list = new ArrayList<>(moves);
		Collections.shuffle(list);
		Move.sort(list, grid, Grid.MoveMetaInformation.WORD_LENGTH_COMPARATOR.reversed());

		return list.get(0);
	}

}
