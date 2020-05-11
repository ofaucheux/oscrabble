package oscrabble.player.ai;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.controller.MicroServiceDictionary;
import oscrabble.controller.MicroServiceScrabbleServer;
import oscrabble.data.GameState;
import oscrabble.data.Player;
import oscrabble.data.objects.Grid;

import java.net.URI;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
		final AIPlayer player = new AIPlayer(game, bfm, PLAYER_NAME);
		final UUID playerUUID = server.addPlayer(game, player.toData());
		server.startGame(game);

		final Callable<Void> test = () -> {
			try
			{
				GameState state;
				do
				{
					state = server.getState(game);
					if (playerUUID.equals(state.getPlayerOnTurn()))
					{
						bfm.grid = Grid.fromData(state.getGrid());
						final Player player0 = state.getPlayers().get(0);
						final ArrayList<String> moves = new ArrayList<>(bfm.getLegalMoves(player0.rack.tiles));
						moves.sort((o1, o2) -> o1.length() - o2.length());
						System.out.println("ici");
						server.play(game, player.buildAction(moves.get(0)));
					}
					Thread.sleep(500);
				} while (state.state != GameState.State.ENDED);
			}
			catch (Throwable e)
			{
				LOGGER.error(e.toString(), e);
			}
			return null;
		};

		final ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.submit(test);
		executor.awaitTermination(60, TimeUnit.SECONDS);
	}
}