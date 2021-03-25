package oscrabble.player.ai;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.controller.MicroServiceDictionary;
import oscrabble.controller.MicroServiceScrabbleServer;
import oscrabble.data.IDictionary;

import java.net.URI;
import java.util.UUID;

class AIPlayerTest {

	public static final Logger LOGGER = LoggerFactory.getLogger(AIPlayerTest.class);

	@BeforeAll
	static void before() {
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> LOGGER.error(t.toString(), e));
	}

	@Test
	void onPlayRequired() throws InterruptedException {

		final IDictionary DICTIONARY = new FrenchDictionaryForTest();
		final MicroServiceScrabbleServer server = MicroServiceScrabbleServer.getLocal();

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