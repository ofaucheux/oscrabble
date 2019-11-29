package oscrabble.player;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
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
	private float mean = .7f;
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

		final int minScore = list.get(0).metaInformation.getScore();
		final int maxScore = list.get(list.size() - 1).metaInformation.getScore();

		final double gaussian = RANDOM.nextGaussian() / 3;
		int selectedScore = (int) ((gaussian + this.mean) * (maxScore + minScore));
		int selected = ListUtils.indexOf(list, el -> el.metaInformation.getScore() >= selectedScore);
		if (selected == -1)
		{
			selected = 0;
		}

		LOGGER.trace("select(): select " + selected + "th of " + list.size() + " moves (Gaussian: " + gaussian + ")");
		return list.get(selected).move;
	}

	private static class Element
	{
		Move move;
		Grid.MoveMetaInformation metaInformation;
	}

}
