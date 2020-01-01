package oscrabble.client;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import oscrabble.Grid;
import oscrabble.Tile;
import oscrabble.dictionary.DictionaryTest;
import oscrabble.server.DummyGame;

import javax.swing.*;
import java.awt.*;

public class PlaygroundTest
{

	@Test
	void displayGrid() throws InterruptedException
	{
		final Grid grid = new Grid(Grid.SCRABBLE_SIZE);
		grid.set(grid.getSquare(1, 3), Tile.SIMPLE_GENERATOR.generateStone('A'));
		showGrid(grid);
	}

	public static void showGrid(final Grid grid) throws InterruptedException
	{
		final Playground.JGrid JGrid = new Playground.JGrid(grid, DictionaryTest.getTestDictionary());
		final JFrame f = new JFrame();
		f.setLayout(new BorderLayout());
		f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		f.add(JGrid);
		JGrid.setPreferredSize(new Dimension(800, 800));
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

		Thread.sleep(3000L);

		final JList<Object> list = new JList<>();
		list.setPreferredSize(new Dimension(400, 0));
		f.add(list, BorderLayout.EAST);
		f.setSize(1200,800);


		Thread.sleep(3000L);

		f.dispose();
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

	@Test
	public void display() throws InterruptedException
	{
		final Playground playground = new Playground();
		final DummyGame game = new DummyGame();
		game.addPlayer(new SwingPlayer("Koala"));
		game.addPlayer(new SwingPlayer("Rhesusaffe"));
		playground.setGame(game);
		try
		{
			game.play();
			Thread.sleep(10 * 1000);

		}
		finally
		{
			playground.gridFrame.dispose();
		}
	}
}
