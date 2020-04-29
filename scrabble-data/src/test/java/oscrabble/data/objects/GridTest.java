package oscrabble.data.objects;

import javafx.geometry.HorizontalDirection;
import org.junit.jupiter.api.Test;
import oscrabble.ScrabbleException;
import oscrabble.data.ScrabbleRules;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GridTest
{
	@Test
	public void notation() throws ScrabbleException.ForbiddenPlayException
	{
		final Grid g = new Grid();
		assertEquals("A1", Grid.Coordinate.getNotation(g.get(1, 1), Grid.Direction.HORIZONTAL));

		assertEquals("H8", Grid.Coordinate.getNotation(g.getCentralSquare(), Grid.Direction.HORIZONTAL));

		final Grid.Coordinate b4 = Grid.getCoordinate("B4");
		assertEquals(2, b4.x);
		assertEquals(4, b4.y);
	}

	@Test
	public void neighbours()
	{
		final Grid grid = new Grid();
		Set<Grid.Square> neighbours;

		neighbours = grid.get(4, 3).getNeighbours();
		assertEquals(4, neighbours.size());
		assertTrue(neighbours.contains(grid.get(4, 4)));

		neighbours = grid.get(0, 4).getNeighbours();
		assertEquals(3, neighbours.size());

		neighbours = grid.get(grid.getSize() - 1, grid.getSize() - 1).getNeighbours();
		assertEquals(2, neighbours.size());
	}

	@Test
	public void jokers() throws ScrabbleException.ForbiddenPlayException
	{
		final Grid grid = new Grid();
		assertEquals(3, grid.get("F6").letterBonus);
		assertEquals(3, grid.get("A1").wordBonus);
	}
}