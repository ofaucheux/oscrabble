package oscrabble.server;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;
import oscrabble.ScrabbleException;
import oscrabble.data.objects.Grid;
import oscrabble.server.action.Action;

import static org.junit.jupiter.api.Assertions.*;

class GridTest
{

	@Test
	void getCoordinate() throws ScrabbleException.ForbiddenPlayException
	{
		final Grid.Coordinate coordinate = Grid.getCoordinate("B1");
		assertEquals(Grid.Direction.HORIZONTAL, coordinate.direction);
		assertEquals(1, coordinate.x);
		assertEquals(2, coordinate.y);

		Grid.getCoordinate("15E");
	}
}