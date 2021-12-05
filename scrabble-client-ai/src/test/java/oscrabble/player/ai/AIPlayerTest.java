package oscrabble.player.ai;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.ScrabbleException;
import oscrabble.controller.ScrabbleServerInterface;
import oscrabble.data.IDictionary;
import oscrabble.server.Game;
import oscrabble.server.Server;

import java.util.UUID;

class AIPlayerTest {

	public static final Logger LOGGER = LoggerFactory.getLogger(AIPlayerTest.class);

	@BeforeAll
	static void before() {
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> LOGGER.error(t.toString(), e));
	}

	/**
	 * Todo: This test currently use a running server. It is to replace by a mock server.
	 * @throws InterruptedException
	 */
	@Test
	@Disabled
	void onPlayRequired() throws InterruptedException, ScrabbleException {

		final IDictionary DICTIONARY = new FrenchDictionaryForTest();
		final ScrabbleServerInterface server = new Server();

		final BruteForceMethod bfm = new BruteForceMethod(DICTIONARY);
		final String PLAYER_NAME = "AI Player";
		final UUID game = server.newGame();

		AIPlayer player = null;
		for (int i = 0; i < 2; i++) {
			final UUID playerID = server.addPlayer(game, PLAYER_NAME);
			player = new AIPlayer(bfm, game, playerID, server);
			player.startDaemonThread();
		}

		server.startGame(game);

		do {
			Thread.sleep(500);
		}
		while (/*server.getState(game).getState() != GameState.State.ENDED && */player.daemonThread.isAlive());
	}
}