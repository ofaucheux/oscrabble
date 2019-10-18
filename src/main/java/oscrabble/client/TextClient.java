package oscrabble.client;

import oscrabble.Grid;

public class TextClient
{
	private final Grid grid;

	public TextClient(final Grid grid)
	{
		this.grid = grid;
	}

	public void refreshGrid()
	{
		System.out.println(this.grid.asASCIIArt());
	}
}
