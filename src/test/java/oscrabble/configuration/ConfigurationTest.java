package oscrabble.configuration;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;

class ConfigurationTest
{

	public static final Logger LOGGER = Logger.getLogger(ConfigurationTest.class);

	@Test
	void configuration() throws InterruptedException, IOException
	{
		final Config config1 = new Config();

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

		LOGGER.info(config1.allowError);
		LOGGER.info(config1.happiness);

		final Properties properties = config1.asProperties();
		final StringWriter sw = new StringWriter();
		properties.list(new PrintWriter(sw));
		LOGGER.info(sw.toString());

		properties.clear();
		final Config config2 = new Config();
		properties.load(new StringReader(sw.toString()));
		config2.loadProperties(properties);
		Assert.assertEquals(config1.happiness, config2.happiness);
	}

	private static class Config extends Configuration
	{
		@Parameter(label = "Allow error")
		boolean allowError;

		@Parameter(label = "Happiness", isSlide = true)
		int happiness;

		@Parameter(label = "Sadless", isSlide = true, lowerBound = -50, upperBound = 100)
		int sadless;

		@Parameter(label = "Seconds", isSlide = false, lowerBound = 0, upperBound = 100)
		int seconds;

		@Parameter(label = "Birth day")
		LocalDate birthDay;

		@Parameter(label = "Wedding day")
		LocalDate wedding;
	}
}