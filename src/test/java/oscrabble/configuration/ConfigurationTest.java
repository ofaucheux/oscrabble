package oscrabble.configuration;

import org.junit.jupiter.api.Test;

import javax.swing.*;

public class ConfigurationTest
{

	@Test
	void test()
	{
		final Configuration configuration = new TestParam();
		JOptionPane.showMessageDialog(null, configuration.createPanel());
	}

	public static class TestParam extends Configuration
	{
		@Parameter(description = "Willst du es?")
		public boolean errorAccepted;

		@Parameter(description = "name")
		public String name;

		@Parameter(description = "score")
		public int score;

		@Override
		public String toString()
		{
			return "errorAccepted: " + errorAccepted + "\nname: " + name + "\nscore: " + score;
		}
	}
}