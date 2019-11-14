package oscrabble.configuration;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.util.Random;

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
	void test()
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