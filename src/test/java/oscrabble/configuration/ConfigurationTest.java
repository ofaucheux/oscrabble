package oscrabble.configuration;

import javafx.scene.control.ButtonBar;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;

class ConfigurationTest
{

	@Test
	void configuration() throws InterruptedException
	{
		final Config1 config1 = new Config1();

		final AtomicBoolean closed = new AtomicBoolean(false);
		final WindowAdapter closeAdapter = new WindowAdapter()
		{
			@Override
			public void windowClosing(final WindowEvent e)
			{
				closed.set(true);
			}
		};

		for (int i = 0; i < 3; i++)
		{
			final int fixI = i;
			new Thread(() -> {
				final JFrame frame = new JFrame();
				frame.add(new ConfigurationPanel(config1));
				frame.setLocationRelativeTo(null);
				final Point location = frame.getLocation();
				location.translate(300 * fixI, 100 * fixI);
				frame.setLocation(location);
				frame.pack();
				frame.setVisible(true);
				frame.addWindowListener(closeAdapter);
			}).start();
		}

		final long start = System.currentTimeMillis();
		while (System.currentTimeMillis() - start < 30000)
		{
			if (closed.get())
			{
				break;
			}
			Thread.sleep(100);
		}

		System.out.println(config1.data);
		System.out.println(config1.allowError);
		System.out.println(config1.happines.getValue());
	}

	private static class Config1 extends Configuration
	{
		@Parameter(label = "Date")
		ButtonBar.ButtonData data;

		@Parameter(label = "Allow error")
		boolean allowError;

		@Parameter(label = "Happiness")
		Range happines = new Range(0, 100, 80);
	}
}