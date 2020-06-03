package oscrabble.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import oscrabble.data.GameState;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

public class PlaygroundTest
{
	@Test
	void displayGrid() throws IOException, InterruptedException
	{
		final Playground playground = new Playground(null);

		final InputStream resourceAsStream = PlaygroundTest.class.getResourceAsStream("game_1.json");
		final GameState state = new ObjectMapper().readValue(resourceAsStream, GameState.class);
		playground.refreshUI(state, Collections.emptyList());

		playground.gridFrame.setVisible(true);
		while (playground.gridFrame.isVisible())
		{
			//noinspection BusyWait
			Thread.sleep(100);
		}
	}
}
