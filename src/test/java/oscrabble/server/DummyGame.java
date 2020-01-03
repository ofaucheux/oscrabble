package oscrabble.server;

import java.io.IOException;

/**
 * Game for test purposes
 */
public class DummyGame extends Game
{

	public DummyGame() throws IOException, ConfigurationException
	{
		super(Game.DEFAULT_PROPERTIES_FILE);
	}
}
