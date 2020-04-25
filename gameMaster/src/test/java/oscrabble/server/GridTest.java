package oscrabble.server;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;
import oscrabble.ScrabbleException;
import oscrabble.server.action.Action;

import static org.junit.jupiter.api.Assertions.*;

class GridTest
{

	@Test
	void getCoordinate() throws ScrabbleException.ForbiddenPlayException
	{
		Triple<Action.Direction, Integer, Integer> coordinate;
		coordinate = Grid.getCoordinate("B1");
		assertEquals(Action.Direction.HORIZONTAL, coordinate.getLeft());
		assertEquals(1, coordinate.getMiddle());
		assertEquals(2, coordinate.getRight());

		Grid.getCoordinate("15E");
	}
}