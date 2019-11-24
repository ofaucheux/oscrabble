package oscrabble.configuration;

import javafx.scene.control.ButtonBar;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;

class ConfigurationTest
{

	@Test
	void configuration() throws InterruptedException
	{
		final Config1 config1 = new Config1();
		for (int i = 0; i < 3; i++)
		{
			final int fixI = i;
			new Thread(() -> {
				final JFrame frame = new JFrame();
				frame.add(new ConfigurationPanel(config1));
				frame.setLocationRelativeTo(null);
				final Point location = frame.getLocation();
				location.translate(200 * fixI, 0);
				frame.setLocation(location);
				frame.pack();
				frame.setVisible(true);
			}).start();
		}
		Thread.sleep(10000);
		System.out.println(config1.data);
		System.out.println(config1.allowError);
	}

	private static class Config1 extends Configuration
	{
		@Parameter(label = "Date")
		ButtonBar.ButtonData data;

		@Parameter(label = "Allow error")
		boolean allowError;
	}
}