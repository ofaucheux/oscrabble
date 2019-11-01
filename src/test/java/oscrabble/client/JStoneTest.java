package oscrabble.client;

import org.junit.jupiter.api.Test;
import oscrabble.Stone;

import javax.swing.*;
import java.awt.*;

class JStoneTest
{

	@Test
	public void test() throws InterruptedException
	{
		final JStone stone = new JStone(new Stone('A', 1));
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