package oscrabble.configuration;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.util.Random;

class ConfigurationTest
{

	private static final Random RANDOM = new Random();

	@Test
	void test()
	{
		final TestConfiguration configuration = new TestConfiguration();
		new SwingWorker<>()
		{
			@Override
			protected Object doInBackground() throws Exception
			{
				for (int i = 0; i < 100; i++)
				{
					configuration.setValue("name", "content " + i);
					configuration.setValue("errorAccepted", !configuration.errorAccepted);
					configuration.setValue("score", RANDOM.nextInt(100));
					configuration.setValue("color", configuration.color == TestConfiguration.MyColor.YELLOW ? TestConfiguration.MyColor.BLUE : TestConfiguration.MyColor.YELLOW);
					Thread.sleep(200);
				}
				return null;
			}
		}.execute();

		JOptionPane.showMessageDialog(null, configuration.createPanel());
	}

	public static class TestConfiguration extends Configuration
	{
		@Parameter(label = "Willst du es?")
		boolean errorAccepted;

		@Parameter(label = "name")
		String name;

		@Parameter(label = "score", description = "points obtained all along the game")
		int score;

		@Parameter(label="color")
		MyColor color;

		private enum MyColor
		{BLUE, GREEN, YELLOW}


		@Override
		public String toString()
		{
			return "errorAccepted: " + this.errorAccepted + "\nname: " + this.name + "\nscore: " + this.score;
		}
	}
}