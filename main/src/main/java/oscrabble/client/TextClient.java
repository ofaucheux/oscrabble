package oscrabble.client;

import org.apache.log4j.Logger;
import oscrabble.Grid;

public class TextClient
{
	public static final Logger LOGGER = Logger.getLogger(TextClient.class);
	private final Grid grid;

	public TextClient(final Grid grid)
	{
		this.grid = grid;
	}

	public void refreshGrid()
	{
		LOGGER.debug(this.grid.asASCIIArt());
	}
}
