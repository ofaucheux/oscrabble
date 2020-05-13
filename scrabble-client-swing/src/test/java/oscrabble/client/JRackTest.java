package oscrabble.client;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import oscrabble.data.Tile;

import javax.swing.*;
import java.util.ArrayList;

/**
 * Test class
 */
class JRackTest
{

	/**
	 * Display a rack.
	 */
	@Test
	@Disabled
	void setTiles()
	{
		final ArrayList<Tile> tiles = new ArrayList<>();
		tiles.add(Tile.builder().c('A').points(1).isJoker(false).build());
		tiles.add(Tile.builder().c('X').points(10).isJoker(false).build());
		tiles.add(Tile.builder().c(' ').points(0).isJoker(true).build());
		final JRack rack = new JRack();
		rack.setTiles(tiles);
		JOptionPane.showMessageDialog(null, rack);
	}
}