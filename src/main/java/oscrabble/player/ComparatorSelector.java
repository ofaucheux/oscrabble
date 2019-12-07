package oscrabble.player;

import org.apache.commons.collections4.ListUtils;
import org.apache.log4j.Logger;
import oscrabble.Grid;
import oscrabble.PlayTiles;
import oscrabble.ScrabbleException;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Selector, der einen Move aus einer Liste zurückgibt. Der Move wird aus einer Normaldistribution zufällig gewählt, nachdem sie sortiert wurden.
 */
public class ComparatorSelector implements IMoveSelector
{
	public static final Logger LOGGER = Logger.getLogger(ComparatorSelector.class);
	private final Function<Grid.MoveMetaInformation, Integer> valueFunction;
	private static final Random RANDOM = new Random();
	private float mean = 0.7f;
	private final Supplier<Grid> gridSupplier;

	ComparatorSelector(final Supplier<Grid> gridSupplier, final Function<Grid.MoveMetaInformation, Integer> valueFunction)
	{
		this.gridSupplier = gridSupplier;
		this.valueFunction = valueFunction;
	}

	@Override
	public PlayTiles select(final Set<PlayTiles> playTiles) throws ScrabbleException
	{
		if (playTiles.isEmpty())
		{
			throw new ScrabbleException(ScrabbleException.ERROR_CODE.ASSERTION_FAILED, "Move list is empty");
		}

		final List<Element> list = new ArrayList<>();
		final Grid grid = this.gridSupplier.get();
		for (PlayTiles playTile : playTiles)
		{
			final Element el = new Element();
			el.playTiles = playTile;
			el.metaInformation = grid.getMetaInformation(playTile);
			list.add(el);
		}
		Collections.shuffle(list);
		final Comparator<Element> comparator =
				(o1, o2) -> ComparatorSelector.this.valueFunction.apply(o1.metaInformation) - this.valueFunction.apply(o2.metaInformation);

		list.sort(comparator);

		final int minValue = this.valueFunction.apply(list.get(0).metaInformation);
		final int maxValue = this.valueFunction.apply(list.get(list.size() - 1).metaInformation);

		final double gaussian = RANDOM.nextGaussian() / 3;
		int selectedValue = (int) ((gaussian + this.mean) * (maxValue + minValue));
		int selected = ListUtils.indexOf(list, el -> this.valueFunction.apply(el.metaInformation) >= selectedValue);
		if (selected == -1)
		{
			selected = list.size() - 1;
		}

		LOGGER.trace("select(): select " + selected + "th of " + list.size() + " moves (Gaussian: " + gaussian + ")");
		return list.get(selected).playTiles;
	}

	public void setMean(final float mean)
	{
		this.mean = mean;
	}

	private static class Element
	{
		PlayTiles playTiles;
		Grid.MoveMetaInformation metaInformation;
	}

}