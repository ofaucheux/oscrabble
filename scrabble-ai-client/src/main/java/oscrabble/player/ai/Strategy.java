package oscrabble.player.ai;


import oscrabble.server.IGame;

import java.util.Collections;
import java.util.List;

/**
 * Playing strategy for a player
 */
@SuppressWarnings("unused")
public abstract class Strategy
{
	private final String label;

	Strategy(final String label)
	{
		this.label = label;
	}

	/**
	 * Sort a list of moves, the better the first.
	 *
	 * @param moves moves
	 */
	abstract void sort(final List<String> moves);

	@Override
	public String toString()
	{
		return this.label;
	}

	public static class BestScore extends Strategy
	{
		final IGame game;

		public BestScore(final IGame game)
		{
			super("BEST SCORE");
			this.game = game;
		}

		@Override
		void sort(final List<String> moves)
		{
			Collections.shuffle(moves);
//			game.getScores(moves); TODO
		}
	}
}

