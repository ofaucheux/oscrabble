package oscrabble.client;

import org.junit.jupiter.api.Test;

import javax.swing.*;

class JTileTest
{

	@Test
	public void test() throws InterruptedException
	{
		final JTile stone = new JTile('A', 1, false);
		final JFrame frame = new JFrame("Test JStone");
//		frame.setUndecorated(true);
//		frame.setBackground(new Color(0, 0, 0, 0));
		frame.add(stone);
		frame.setVisible(true);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		while (frame.isVisible())
		{
			Thread.sleep(100);
		}
	}
}