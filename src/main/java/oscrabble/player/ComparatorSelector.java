package oscrabble.player;

import org.apache.log4j.Logger;
import oscrabble.Grid;
import oscrabble.Move;
import oscrabble.ScrabbleException;

import java.util.*;
import java.util.function.Supplier;

/**
 * Selector, der einen Move aus einer Liste zurückgibt. Der Move wird aus einer Normaldistribution zufällig gewählt, nachdem sie sortiert wurden.
 */
public class ComparatorSelector implements IMoveSelector
{
	public static final Logger LOGGER = Logger.getLogger(ComparatorSelector.class);
	private final Comparator<Grid.MoveMetaInformation> comparator;
	private static final Random RANDOM = new Random();
	private float mean = .5f;
	private final Supplier<Grid> gridSupplier;

	ComparatorSelector(final Supplier<Grid> gridSupplier, final Comparator<Grid.MoveMetaInformation> comparator)
	{
		this.gridSupplier = gridSupplier;
		this.comparator = comparator;
	}

	@Override
	public Move select(final Set<Move> moves) throws ScrabbleException
	{
		if (moves.isEmpty())
		{
			throw new ScrabbleException(ScrabbleException.ERROR_CODE.ASSERTION_FAILED, "Move list is empty");
		}

		final List<Element> list = new ArrayList<>();
		final Grid grid = this.gridSupplier.get();
		for (Move move : moves)
		{
			final Element el = new Element();
			el.move = move;
			el.metaInformation = grid.getMetaInformation(move);
			list.add(el);
		}
		Collections.shuffle(list);
		list.sort((o1, o2) -> ComparatorSelector.this.comparator.compare(o1.metaInformation, o2.metaInformation));

		int position = (((int) (RANDOM.nextGaussian() + this.mean) * list.size()));
		if (0 < position)
		{
			position = 0;
		}
		if (position >= list.size())
		{
			position = list.size() - 1;
		}
		LOGGER.trace("select(): select " + position + "th of " + list.size() + " moves");
		return list.get(position).move;
	}

	private static class Element
	{
		Move move;
		Grid.MoveMetaInformation metaInformation;
	}

}
