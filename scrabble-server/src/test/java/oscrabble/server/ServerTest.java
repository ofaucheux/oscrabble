package oscrabble.server;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import oscrabble.data.GameState;
import oscrabble.data.fixtures.PrecompiledGameStates;

public class ServerTest {
	@Test
	public void loadJsonGame() {
		final GameState game1 = PrecompiledGameStates.game1();
		final Game game = new Game(game1);
		Assertions.assertEquals(game.getGameState(), game1);
	}
}

