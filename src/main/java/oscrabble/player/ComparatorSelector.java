package oscrabble.player;

import oscrabble.Grid;
import oscrabble.Move;
import oscrabble.ScrabbleException;

import java.util.*;

/**
 * Filter, das nur Wort mit den meisten neuen Buchstaben übrig lässt.
 */
public class ComparatorSelector implements IMoveSelector
{
	private final Grid grid;
	private final Comparator<Grid.MoveMetaInformation> comparator;

	public ComparatorSelector(final Grid grid, final Comparator<Grid.MoveMetaInformation> comparator)
	{
		this.grid  = grid;
		this.comparator = comparator;
	}

	@Override
	public Move select(final Set<Move> moves) throws ScrabbleException
	{
		final List<Move> list = new ArrayList<>(moves);
		Collections.shuffle(list);
		Move.sort(list, grid, comparator.reversed());

		return list.get(0);
	}

}
