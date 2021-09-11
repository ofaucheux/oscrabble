package oscrabble.client.ui;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.swing.*;

import java.awt.*;

@SuppressWarnings("HardCodedStringLiteral")
class CursorImageTest {

	@Test
	@Disabled
		// disable swing tests
	void create() throws InterruptedException {
		final JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.setSize(200, 200);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);

		final JButton button = new JButton("Test");
		button.setSize(100, 50);
		frame.add(button);

		new CursorImage("Émeraude", Color.BLUE)
				.setOnWindow(frame);

		while (frame.isVisible()) {
			Thread.sleep(100);
		}
	}
}