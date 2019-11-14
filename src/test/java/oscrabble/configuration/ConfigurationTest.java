package oscrabble.configuration;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ConfigurationUtils;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.util.Random;

import static org.junit.Assert.*;

class ConfigurationTest implements Configurable
{

	private static final Random RANDOM = new Random();

	@Parameter(label = "Willst du es?")
	private	boolean errorAccepted;

	@Parameter(label = "name")
	private	String name;

	@Parameter(label = "score", description = "points obtained all along the game")
	private	int score;

	@Parameter(label="color")
	private	MyColor color;


	@Parameter(label="second color")
	private	MyColor color2;

	@Test
	void testConfiguration()
	{
		final PropertiesConfiguration config = new PropertiesConfiguration();
		for (final String prefix : new String[]{null, "myPrefix"})
		{
			final Configuration subconfig = prefix == null ? config : config.subset(prefix);
			subconfig.setProperty("color", MyColor.BLUE);
			subconfig.setProperty("score", 2010);
			subconfig.setProperty("errorAccepted", false);
			ConfigurableUtils.loadProperties(this, subconfig);
		}

		assertEquals(MyColor.BLUE, color);
		assertEquals(2010, score);
		assertEquals(false, errorAccepted);
	}

	@Test
	void testPanel()
	{
		new SwingWorker<>()
		{
			@Override
			protected Object doInBackground() throws Exception
			{
				for (int i = 0; i < 10; i++)
				{
					ConfigurationTest.this.name =  "content " + i;
					ConfigurationTest.this.errorAccepted = !ConfigurationTest.this.errorAccepted;
					ConfigurationTest.this.score = RANDOM.nextInt(100);
					ConfigurationTest.this.color = ConfigurationTest.this.color == MyColor.YELLOW ? MyColor.BLUE : MyColor.YELLOW;
					ConfigurationTest.this.firePropertiesChange();
					Thread.sleep(200);
				}

				for (int i = 0; i < 10; i++)
				{
					ConfigurationTest.this.color2 = ConfigurationTest.this.color2 == MyColor.YELLOW ? MyColor.BLUE : MyColor.YELLOW;
					ConfigurationTest.this.firePropertiesChange();
					Thread.sleep(200);
				}
				JOptionPane.showMessageDialog(null, "Done!");
				return null;
			}
		}.execute();

		JOptionPane.showMessageDialog(null, new ConfigurationPanel(this));


	}


	private enum MyColor
	{BLUE, YELLOW}


	@Override
	public String toString()
	{
		return "errorAccepted: " + this.errorAccepted + "\nname: " + this.name + "\nscore: " + this.score;
	}
}