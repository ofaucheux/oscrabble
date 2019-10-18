package oscrabble.client;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import oscrabble.Grid;
import oscrabble.Stone;
import oscrabble.dictionary.Dictionary;
import oscrabble.dictionary.DictionaryTest;
import oscrabble.server.ScrabbleServer;
import oscrabble.server.ScrabbleServerTest;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;

public class SwingClientTest
{
	private Grid grid;

	@Test
	void layout()
	{
		this.grid = new Grid(Grid.SCRABBLE_SIZE);
		this.grid.set(this.grid.getSquare(1, 3), Stone.SIMPLE_GENERATOR.generateStone('A'));
		showGrid(grid);
	}

	public static void showGrid(final Grid grid)
	{
		final SwingClient.JGrid JGrid = new SwingClient.JGrid(grid, DictionaryTest.getTestDictionary());
		final JFrame f = new JFrame();
		f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		f.add(JGrid);
		f.setVisible(true);
		f.setSize(800,800);

		final Thread th = new Thread(() -> {
			try
			{
				while (true)
				{
					Thread.sleep(100);
					JGrid.repaint();
				}
			}
			catch (InterruptedException e)
			{
				throw new Error();
			}
		});
		th.setDaemon(true);
		th.start();
	}

	@Test
	public void display()
	{
		final ScrabbleServer server = ScrabbleServerTest.getTestServer();
		final SwingClient client = new SwingClient(server, "Test Client");
		server.register(client);
		server.prepareGame();
		client.setCommandPrompt("H8 BISOU");
	}

	@AfterAll
	static void sleep() throws InterruptedException
	{
		for (final Frame frame : Frame.getFrames())
		{
			while (frame.isVisible())
			{
				Thread.sleep(100);
			}
		}
	}
}
