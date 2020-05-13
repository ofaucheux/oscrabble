package oscrabble.client;

import oscrabble.data.Tile;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Rack of an player.
 */
public class JRack extends JPanel
{
	static final int RACK_SIZE = 7;

	final JRackCell[] cells = new JRackCell[7];

	/**
	 *
	 */
	JRack()
	{
		this.setLayout(new GridLayout(1,7));
		for (int i = 0; i < RACK_SIZE; i++)
		{
			this.cells[i] = new JRackCell();
			add(this.cells[i]);
		}
	}

	void setTiles(List<Tile> tiles)
	{
		for (int i = 0; i < RACK_SIZE; i++)
		{
			this.cells[i].setTile(
					i >= tiles.size() ? null : new JTile(tiles.get(i)));
		}
		this.repaint();
	}
}
