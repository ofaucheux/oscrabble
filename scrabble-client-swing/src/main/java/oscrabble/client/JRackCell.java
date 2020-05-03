package oscrabble.client;

import javax.swing.*;
import java.awt.*;

class JRackCell extends JComponent
{
	private JTile tile;

	JRackCell()
	{
		setPreferredSize(JTile.CELL_DIMENSION);
	}

	@Override
	protected void paintComponent(final Graphics g)
	{
		super.paintComponent(g);
		JTile.drawStone((Graphics2D) g, this, this.tile, Color.black);
	}

	public void setTile(final JTile tile)
	{
		this.tile = tile;
	}
}
