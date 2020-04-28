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
		final Grid g = new Grid(15);
		assertEquals("A1", Grid.Coordinate.getNotation(g.get(0, 0), Grid.Direction.HORIZONTAL));

		assertEquals("H8", Grid.Coordinate.getNotation(g.getCentralSquare(), Grid.Direction.HORIZONTAL));

		final Grid.Coordinate b4 = Grid.getCoordinate("B4");
		assertEquals(1, b4.x);
		assertEquals(3, b4.y);
	}

	@Test
	public void neighbours()
	{
		final Grid grid = new Grid(15);
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