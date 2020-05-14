package oscrabble.data.objects;

import org.junit.jupiter.api.Test;
import oscrabble.ScrabbleException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GridTest
{
	@Test
	public void notation()
	{
		final Grid g = new Grid(null);
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
		Set<Square> neighbours;

		neighbours = grid.getNeighbours(grid.get(4, 3));
		assertEquals(4, neighbours.size());
		assertTrue(neighbours.contains(grid.get(4, 4)));

		neighbours = grid.getNeighbours(grid.get(0, 4));
		assertEquals(3, neighbours.size());

		neighbours = grid.getNeighbours(grid.get(grid.getSize() - 1, grid.getSize() - 1));
		assertEquals(2, neighbours.size());
	}

	@Test
	public void jokers()
	{
		final Grid grid = new Grid();
		assertEquals(3, grid.get("F6").letterBonus);
		assertEquals(3, grid.get("A1").wordBonus);
	}

	@Test
	public void getWords() throws ScrabbleException.ForbiddenPlayException
	{
		final Grid g = new Grid();
		g.play(null, "G3 RHuME");
		g.play(null, "2H CHIEN");
		final Set<String> words = g.getWords("H3");
		assertEquals(g.getWords("3H"), words);
		assertEquals(new HashSet<>(Arrays.asList("CHIEN", "RHUME")), words);
		assertEquals(new HashSet<>(Arrays.asList("RHUME")), g.getWords("G3"));

		assertTrue(g.getWords("B3").isEmpty());
		assertTrue(g.getWords("A1").isEmpty());
	}
}