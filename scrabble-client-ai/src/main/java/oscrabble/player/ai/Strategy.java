package oscrabble.player.ai;

import oscrabble.ScrabbleException;
import oscrabble.controller.ScrabbleServerInterface;
import oscrabble.data.Score;

import java.util.*;
import java.util.function.Function;

/**
 * Playing strategy for a player
 */
public abstract class Strategy {

	protected static TreeMap<Integer, List<String>> sortByFunction(
			final Collection<String> notations,
			final Function<String, Integer> strategyScoreFunction,
			final Comparator<String> secondComparator
	) {
		final TreeMap<Integer, List<String>> scoreMap = new TreeMap<>();
		for (final String notation : notations) {
			final Integer strategyScore = strategyScoreFunction.apply(notation);
			scoreMap.computeIfAbsent(strategyScore, (k) -> new ArrayList<>())
					.add(notation);
		}
		if (secondComparator != null) {
			for (final Integer value : scoreMap.keySet()) {
				scoreMap.get(value).sort(secondComparator);
			}
		}

		return scoreMap;
	}

	public abstract TreeMap<Integer, List<String>> sort(final Set<String> moves);

	/**
	 * Strategy: best scores first
	 */
	public static class BestScore extends Strategy {
		private ScrabbleServerInterface server;
		private UUID game;

		public BestScore(final ScrabbleServerInterface server, final UUID game) {
			this.server = server;
			this.game = game;
		}

		public void setGame(final UUID game) {
			this.game = game;
		}

		public void setServer(final ScrabbleServerInterface server) {
			this.server = server;
		}

		@Override
		public TreeMap<Integer, List<String>> sort(final Set<String> moves) {
			try {
				final Collection<Score> scores = this.server.getScores(this.game, moves);
				return sort(moves, scores);
			} catch (ScrabbleException e) {
				throw new Error(e);
			}
		}

		private TreeMap<Integer, List<String>> sort(final Set<String> moves, Collection<Score> scores) {
			final HashMap<String, Integer> map = new HashMap<>();
			for (final Score score : scores) {
				map.put(score.getNotation(), score.getScore());
			}
			return Strategy.sortByFunction(
					moves,
					w -> map.get(w),
					(a,b) -> Score.getWord(b).length() - Score.getWord(a).length()
			);
		}
	}

	public static class BestSize extends Strategy {

		@Override
		public TreeMap<Integer, List<String>> sort(final Set<String> moves) {
			return Strategy.sortByFunction(
					moves,
					m -> m.length(),
					null
			);
		}
	}
}