package oscrabble.player.ai;

import org.junit.jupiter.api.Test;
import oscrabble.ScrabbleException;
import oscrabble.controller.Action;
import oscrabble.controller.MicroServiceDictionary;
import oscrabble.server.AbstractGameListener;
import oscrabble.server.Game;

import java.net.URI;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

class AIPlayerTest
{

	@Test
	void onPlayRequired() throws ScrabbleException, TimeoutException, InterruptedException
	{
		final MicroServiceDictionary DICTIONARY = new MicroServiceDictionary(URI.create("http://localhost:8080/"), "FRENCH");

		final Game game = new Game(DICTIONARY);
		final BruteForceMethod bfm = new BruteForceMethod(DICTIONARY);
		final AIPlayer player = new AIPlayer(bfm, "AI Player");
		game.addPlayer(player.toData());
//		final ArrayBlockingQueue<String> playQueue = new ArrayBlockingQueue<>(100);
		game.addListener(new AbstractGameListener()
		{
			@Override
			public void onPlayRequired(final Game.Player player)
			{
				try
				{
					bfm.grid = game.getGrid();
					final ArrayList<String> moves = new ArrayList<>(bfm.getLegalMoves(game.getRack(player)));
					moves.sort((o1, o2) -> o1.length() - o2.length());
					game.play(player, Action.parse(moves.get(moves.size() - 1)));
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