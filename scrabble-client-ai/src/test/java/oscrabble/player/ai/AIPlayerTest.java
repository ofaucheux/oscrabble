package oscrabble.player.ai;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import oscrabble.ScrabbleException;
import oscrabble.controller.Action;
import oscrabble.controller.MicroServiceDictionary;
import oscrabble.data.GameState;
import oscrabble.data.objects.Grid;
import oscrabble.server.AbstractGameListener;
import oscrabble.server.Controller;
import oscrabble.server.Game;

import java.net.URI;
import java.util.ArrayList;
import java.util.concurrent.*;

class AIPlayerTest
{

	public static final Logger LOGGER = LoggerFactory.getLogger(AIPlayerTest.class);

	@BeforeAll
	static void before()
	{
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler()
		{
			@Override
			public void uncaughtException(final Thread t, final Throwable e)
			{
				LOGGER.error(t.toString(), e);
			}
		});
	}

	@Test
	void onPlayRequired() throws InterruptedException
	{

		final MicroServiceDictionary DICTIONARY = new MicroServiceDictionary(URI.create("http://localhost:8080/"), "FRENCH");

		final Controller controller = new Controller(new Game(DICTIONARY));
		final BruteForceMethod bfm = new BruteForceMethod(DICTIONARY);
		final String PLAYER_NAME = "AI Player";
		final AIPlayer player = new AIPlayer(bfm, PLAYER_NAME);
		controller.addPlayer(player.toData());
		controller.startGame();

		final Callable<Void> test = () -> {
			try
			{
				GameState gameState;
				do
				{
					gameState = controller.getState().getBody();
					LOGGER.info(gameState.toString());
					if (PLAYER_NAME.equals(gameState.getPlayerOnTurn()))
					{
						bfm.grid = Grid.fromData(gameState.getGrid());
						final ArrayList<String> moves = new ArrayList<>(bfm.getLegalMoves(gameState.getPlayers().get(0).rack.tiles));
						moves.sort((o1, o2) -> o1.length() - o2.length());
						System.out.println("ici");
						//						game.play(player, Action.parse(moves.get(moves.size() - 1)));
					}
					Thread.sleep(500);
				} while (gameState.state != GameState.State.ENDED);
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