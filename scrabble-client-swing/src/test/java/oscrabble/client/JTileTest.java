package oscrabble.client;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;

class JTileTest {

	@Test
	@Disabled
	public void test() throws InterruptedException {
		final JFrame frame = new JFrame("Test JStone");
		frame.setLayout(new GridLayout(2, 2));
//		frame.setUndecorated(true);
//		frame.setBackground(new Color(0, 0, 0, 0));

		JTile stone;
		stone = new JTile('A', 1, false);
		frame.add(stone);
		stone = new JTile('Y', 10, false);
		frame.add(stone);
		stone = new JTile('c', 0, true);
		frame.add(stone);

		frame.setVisible(true);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		while (frame.isVisible()) {
			Thread.sleep(100);
		}
	}
}