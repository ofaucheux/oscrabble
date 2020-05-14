package oscrabble.player.ai;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.ScrabbleException;
import oscrabble.controller.MicroServiceDictionary;
import oscrabble.controller.MicroServiceScrabbleServer;

import java.net.URI;
import java.util.UUID;

class AIPlayerTest
{

	public static final Logger LOGGER = LoggerFactory.getLogger(AIPlayerTest.class);

	@BeforeAll
	static void before()
	{
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> LOGGER.error(t.toString(), e));
	}

	@Test
	void onPlayRequired() throws InterruptedException
	{

		final MicroServiceDictionary DICTIONARY = new MicroServiceDictionary(URI.create("http://localhost:8080/"), "FRENCH");
		final MicroServiceScrabbleServer server = new MicroServiceScrabbleServer(URI.create("http://localhost:" + MicroServiceScrabbleServer.DEFAULT_PORT));

		final BruteForceMethod bfm = new BruteForceMethod(DICTIONARY);
		final String PLAYER_NAME = "AI Player";
		final UUID game = server.newGame();
		final UUID playerID = server.addPlayer(game, PLAYER_NAME);
		final AIPlayer player = new AIPlayer(bfm, game, playerID, server);
		server.startGame(game);
		player.startDaemonThread();

		do {
			Thread.sleep(500);
		}
		while (/*server.getState(game).getState() != GameState.State.ENDED && */player.daemonThread.isAlive());
	}
}