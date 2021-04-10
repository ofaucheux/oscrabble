package oscrabble.player.ai;


import org.apache.commons.collections4.iterators.EmptyListIterator;
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
	 * Sort a list of moves, the better the first, and return an iterator on it,
	 * which is positioned on the next wished word.
	 *
	 * @param moves moves
	 */
	public abstract ListIterator<String> sort(final List<String> moves);

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
		public ListIterator sort(final List<String> moves) {
			try {
				if (moves.isEmpty()) {
					return EmptyListIterator.INSTANCE;
				}

				final ArrayList<Score> scores = new ArrayList<>(this.server.getScores(this.game, moves));
				Collections.shuffle(scores);
				scores.sort((a, b) -> b.getScore() - a.getScore());
				final ListIterator<Score> scoreListIterator = scores.listIterator();
				final int maxScore = scoreListIterator.next().getScore();
				final int wishedScore = (maxScore * 7 / 10);
				while (scoreListIterator.hasNext() && scoreListIterator.next().getScore() > wishedScore) {
					scoreListIterator.next();
				}
				if (!scoreListIterator.hasNext()) {
					scoreListIterator.previous();
				}
				moves.clear();
				scores.forEach(sc -> moves.add(sc.getNotation()));
				return moves.listIterator(scoreListIterator.nextIndex());
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
		public ListIterator<String> sort(final List<String> moves) {
			Collections.shuffle(moves);
			moves.sort(LENGTH_COMPARATOR.reversed());
			return moves.listIterator(0);
		}
	}
}