package oscrabble.controller;

import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;
import oscrabble.ScrabbleException;
import oscrabble.data.objects.Coordinate;
import oscrabble.data.objects.Grid;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static oscrabble.data.Action.PASS_TURN_NOTATION;

public abstract class Action {
	private static final Pattern EXCHANGE = Pattern.compile("-\\s+(\\S+)");
	private static final Pattern PLAY_TILES = Pattern.compile("(\\S*)\\s+(\\S*)");

	public UUID turnId;
	public final String notation;
	public final UUID player;

	public Integer score;

	protected Action(final UUID player, final String notation) {
		this.player = player;
		this.notation = notation;
	}

	static public Action parse(oscrabble.data.Action jsonAction) throws ScrabbleException.NotParsableException {
		final Action action = parse(jsonAction.player, jsonAction.notation);
		action.turnId = jsonAction.turnId;
		return action;
	}

	public static Action parse(final UUID player, final String notation) throws ScrabbleException.NotParsableException {
		final Action action;
		if (PASS_TURN_NOTATION.equals(notation)) {
			action = new SkipTurn(player, notation);
		} else if (EXCHANGE.matcher(notation).matches()) {
			action = new Exchange(player, notation);
		} else if (PLAY_TILES.matcher(notation).matches()) {
			action = new PlayTiles(player, notation);
		} else {
			throw new ScrabbleException.NotParsableException(notation);
		}

		return action;
	}

	/**
	 * Parse a notation
	 *
	 * @param notation
	 * @return (coordinate, word)
	 */
	public static Pair<Coordinate, String> parsePlayNotation(final String notation) throws ScrabbleException.NotParsableException {
		if (PASS_TURN_NOTATION.equals(notation)) {
			return Pair.of(null, PASS_TURN_NOTATION);
		}
		final Matcher m = PLAY_TILES.matcher(notation);
		if (!m.matches()) {
			throw new ScrabbleException.NotParsableException(notation);
		}
		return Pair.of(Coordinate.parse(m.group(1)), m.group(2));
	}

	@Override
	public String toString() {
		return "Action{" +
				"notation='" + this.notation + '\'' +
				'}';
	}

	public boolean isSkipTurn() {
		return false;
	}

	public static class SkipTurn extends Action {
		public SkipTurn(final UUID player, final String notation) {
			super(player, notation);
		}

		@Override
		public boolean isSkipTurn() {
			return true;
		}
	}

	/**
	 * Action für den Austausch von Buchstaben.
	 */
	public static class Exchange extends Action {

		public final char[] toExchange;

		private Exchange(final UUID player, final String notation) {
			super(player, notation);
			final String chars = EXCHANGE.matcher(notation).group(1);
			this.toExchange = chars.toCharArray();
		}
	}

	public static class PlayTiles extends Action {
		public final Coordinate startSquare;

		/**
		 * The word created by this move, incl. already set tiles and where blanks are represented by lowercase letters.
		 */
		public String word;

		/**
		 * Die Blanks (mindesten neugespielt) werden durch klein-buchstaben dargestellt.
		 */
		@SneakyThrows
		private PlayTiles(final UUID player, String notation) {
			super(player, notation);
			final Pair<Coordinate, String> parsed = parsePlayNotation(notation);
			this.startSquare = parsed.getLeft();
			this.word = parsed.getRight();
		}

		public Grid.Direction getDirection() {
			return this.startSquare.direction;
		}

		/**
		 * @return the number of columns the word will use
		 */
		public int getWidth() {
			return getDirection() == Grid.Direction.HORIZONTAL ? this.word.length() : 1;
		}

		/**
		 * @return the number of rows the word will use
		 */
		public int getHeight() {
			return getDirection() == Grid.Direction.VERTICAL ? this.word.length() : 1;
		}
	}

	public static class ExchangePoints extends Action {
		public ExchangePoints(final UUID giverPlayer, final String giverPlayerName, final int value) {
			super(giverPlayer, String.format("%s gives %s points", giverPlayerName, value));
		}
	}
}
