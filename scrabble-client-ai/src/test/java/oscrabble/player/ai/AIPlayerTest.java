package oscrabble.player.ai;

import org.junit.jupiter.api.Test;
import oscrabble.ScrabbleException;
import oscrabble.controller.Action;
import oscrabble.controller.MicroServiceDictionary;
import oscrabble.server.AbstractGameListener;
import oscrabble.server.Game;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeoutException;

class AIPlayerTest
{

	public static final Random RANDOM = new Random();

	@Test
	void onPlayRequired() throws ScrabbleException, TimeoutException, InterruptedException
	{
		final MicroServiceDictionary DICTIONARY = new MicroServiceDictionary(URI.create("http://localhost:8080/"), "FRENCH");

		final Game game = new Game(DICTIONARY);
		final BruteForceMethod bfm = new BruteForceMethod(DICTIONARY);
		final AIPlayer player = new AIPlayer(bfm, "AI Player");
		game.addPlayer(player);
//		final ArrayBlockingQueue<String> playQueue = new ArrayBlockingQueue<>(100);
		game.addListener(new AbstractGameListener()
		{
			@Override
			public void onPlayRequired(final Game.Player player)
			{
				try
				{
					bfm.grid = game.getGrid();
					final List<String> moves = new ArrayList<>();
					moves.addAll(bfm.getLegalMoves(game.getRack(player)));
					game.play(player, Action.parse(moves.get(RANDOM.nextInt(moves.size()))));
				}
				catch (ScrabbleException.ForbiddenPlayException | ScrabbleException.NotInTurn e)
				{
					throw new Error(e);
				}
			}
		});
		new Thread(() -> game.play()).start();
		game.awaitEndOfPlay(10);
	}
}