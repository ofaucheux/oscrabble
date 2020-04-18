package oscrabble.server;

import oscrabble.ScrabbleException;

/**
 * Game for test purposes
 */
public class DummyGame extends Game
{

	public DummyGame() throws ScrabbleException
	{
		super(Game.DEFAULT_PROPERTIES_FILE);
	}

	@Override
	public int getNumberTilesInBag()
	{
		throw new AssertionError("Not implemented");
	}
}
