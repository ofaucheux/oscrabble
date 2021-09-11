package oscrabble.server;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import oscrabble.data.GameState;
import oscrabble.data.fixtures.PrecompiledGameStates;

import java.util.UUID;

@SuppressWarnings("HardCodedStringLiteral")
@Disabled
public class ServerTest {
	@Test
	public void loadJsonGame() {
		final GameState game1 = PrecompiledGameStates.game1();
		final Game game = new Game(game1);
		Assertions.assertEquals(game.getGameState(), game1);
	}

	@Test
	public void refusedWords() {
		final Server server = new Server();
		final UUID game = UUID.randomUUID();
		Assertions.assertTrue(server.getAdditionalRefusedWords(game).isEmpty());
		server.addRefusedWord(game, "word1");
		server.addRefusedWord(game, "word2");
		Assertions.assertTrue(server.isRefused(game, "WorD1"));
		Assertions.assertTrue(server.isRefused(game, "WorD2"));
		Assertions.assertFalse(server.isRefused(game, "Word3"));
	}
}