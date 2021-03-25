package oscrabble.player.ai;


import oscrabble.ScrabbleException;
import oscrabble.controller.MicroServiceScrabbleServer;
import oscrabble.data.Score;

import java.util.*;

/**
 * Playing strategy for a player
 */
public abstract class Strategy {
	private final String label;

	protected Strategy(final String label) {
		this.label = label;
	}

	/**
	 * Sort a list of moves, the better the first.
	 *
	 * @param moves moves
	 */
	public abstract void sort(final List<String> moves);

	@Override
	public String toString() {
		return this.label;
	}

	/**
	 * Strategy: best scores first
	 */
	public static class BestScore extends Strategy {
		private MicroServiceScrabbleServer server;
		private UUID game;

		public BestScore(final MicroServiceScrabbleServer server, final UUID game) {
			super("Best score"); // todo: i18n
			this.server = server;
			this.game = game;
		}

		@Override
		public void sort(final List<String> moves) {
			try {
				final ArrayList<Score> scores = new ArrayList<>(this.server.getScores(this.game, moves));
				Collections.shuffle(scores);
				scores.sort((a, b) -> b.getScore() - a.getScore());
				moves.clear();
				scores.forEach(sc -> moves.add(sc.getNotation()));
			} catch (ScrabbleException.CommunicationException e) {
				throw new Error(e);
			}
		}

		public void setGame(final UUID game) {
			this.game = game;
		}

		public void setServer(final MicroServiceScrabbleServer server) {
			this.server = server;
		}
	}

	public static class BestSize extends Strategy {

		public static final Comparator<String> LENGTH_COMPARATOR = (o1, o2) -> o1.length() - o2.length();

		public BestSize() {
			super("Best size"); // todo i18n
		}

		@Override
		public void sort(final List<String> moves) {
			Collections.shuffle(moves);
			moves.sort(LENGTH_COMPARATOR.reversed());
		}
	}
}

