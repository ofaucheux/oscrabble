package oscrabble.data.objects;

import org.junit.jupiter.api.Test;
import oscrabble.data.ScrabbleRules;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GridTest
{

	@Test
	public void neighbours()
	{
		final ScrabbleRules rules = new ScrabbleRules();
		rules.gridSize = 15;
		final Grid grid = new Grid(rules);
		Set<Grid.Square> neighbours;

		neighbours = grid.get(4, 3).getNeighbours();
		assertEquals(4, neighbours.size());
		assertTrue(neighbours.contains(grid.get(4, 4)));

		neighbours = grid.get(0, 4).getNeighbours();
		assertEquals(3, neighbours.size());

		neighbours = grid.get(grid.getSize() - 1, grid.getSize() - 1).getNeighbours();
		assertEquals(2, neighbours.size());
	}
}