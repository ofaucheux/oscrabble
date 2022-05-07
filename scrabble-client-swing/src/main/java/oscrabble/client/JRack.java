package oscrabble.client;

import oscrabble.ScrabbleConstants;
import oscrabble.client.utils.SwingUtils;
import oscrabble.data.Tile;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Rack of a player.
 */
public class JRack extends JPanel {

	final JRackCell[] cells = new JRackCell[7];

	/**
	 * Create the image of a rack containing tiles
	 * @param tiles
	 * @return
	 */
	public static byte[] createImage(int cellSize, List<Tile> tiles) {
		final JRack jRack = new JRack();
		jRack.setTiles(tiles);
		return SwingUtils.getImage(jRack, new Dimension(ScrabbleConstants.RACK_SIZE * cellSize, cellSize));
	}

	/**
	 *
	 */
	JRack() {
		this.setLayout(new GridLayout(1, ScrabbleConstants.RACK_SIZE));
		for (int i = 0; i < ScrabbleConstants.RACK_SIZE; i++) {
			this.cells[i] = new JRackCell();
			add(this.cells[i]);
		}
	}

	void setTiles(List<Tile> tiles) {
		for (int i = 0; i < ScrabbleConstants.RACK_SIZE; i++) {
			this.cells[i].setTile(
					i >= tiles.size() ? null : new JTile(tiles.get(i)));
		}
		this.repaint();
	}
}
