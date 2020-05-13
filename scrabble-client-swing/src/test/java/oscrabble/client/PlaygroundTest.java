package oscrabble.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import oscrabble.data.GameState;

import java.io.IOException;
import java.io.InputStream;

public class PlaygroundTest
{

	@Test
	void displayGrid() throws IOException, InterruptedException
	{
		final Playground playground = new Playground();

		final InputStream resourceAsStream = PlaygroundTest.class.getResourceAsStream("game_1.json");
		final GameState state = new ObjectMapper().readValue(resourceAsStream, GameState.class);
		playground.refreshUI(state);

		playground.gridFrame.setVisible(true);
		while (playground.gridFrame.isVisible())
		{
			Thread.sleep(100);
		}
	}
//
//	public static void showGrid(final Grid grid) throws InterruptedException, ScrabbleException
//	{
//		final Playground.JGrid JGrid = new Playground.JGrid(grid, new ScrabbleLanguageInformation(Language.FRENCH), new DummyGame());
//		final JFrame f = new JFrame();
//		f.setLayout(new BorderLayout());
//		f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
//		f.add(JGrid);
//		JGrid.setPreferredSize(new Dimension(800, 800));
//		f.setVisible(true);
//		f.setSize(800,800);
//
//		final Thread th = new Thread(() -> {
//			try
//			{
//				while (true)
//				{
//					Thread.sleep(100);
//					JGrid.repaint();
//				}
//			}
//			catch (InterruptedException e)
//			{
//				throw new Error();
//			}
//		});
//		th.setDaemon(true);
//		th.start();
//
//		Thread.sleep(3000L);
//
//		final JList<Object> list = new JList<>();
//		list.setPreferredSize(new Dimension(400, 0));
//		f.add(list, BorderLayout.EAST);
//		f.setSize(1200,800);
//
//
//		Thread.sleep(3000L);
//
//		f.dispose();
//	}
//
//	@AfterAll
//	static void sleep() throws InterruptedException
//	{
//		for (final Frame frame : Frame.getFrames())
//		{
//			while (frame.isVisible())
//			{
//				Thread.sleep(100);
//			}
//		}
//	}
//
//	@Test
//	public void display() throws InterruptedException, ScrabbleException
//	{
//		final Playground playground = new Playground();
//		final DummyGame game = new DummyGame();
//		game.addPlayer(new SwingPlayer("Koala"));
//		game.addPlayer(new SwingPlayer("Rhesusaffe"));
//		playground.setGame(game);
//		try
//		{
//			game.play();
//			Thread.sleep(10 * 1000);
//
//		}
//		finally
//		{
//			if (playground.gridFrame != null)
//			{
//				playground.gridFrame.dispose();
//			}
//		}
//	}
}
