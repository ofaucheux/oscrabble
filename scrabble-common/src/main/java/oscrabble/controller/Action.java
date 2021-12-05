package oscrabble.controller;

import org.apache.commons.lang3.tuple.Pair;
import oscrabble.ScrabbleException;
import oscrabble.data.objects.Coordinate;
import oscrabble.data.objects.Grid;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Action {
	private static final Pattern PASS_TURN = Pattern.compile(
			Pattern.quote(oscrabble.data.Action.PASS_TURN_NOTATION)
	);
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
		if (PASS_TURN.matcher(notation).matches()) {
			action = new SkipTurn(player, notation);
		} else if (EXCHANGE.matcher(notation).matches()) {
			action = new Exchange(player, notation);
		} else if (PLAY_TILES.matcher(notation).matches()) {
			action = new PlayTiles(player, notation);
		} else {
			throw new ScrabbleException.NotParsableException("Illegal move notation: \"" + notation + "\"");
		}

		return action;
	}

	/**
	 * Parse a notation
	 *
	 * @param notation
	 * @return Tuplet (coordinate, word)
	 */
	public static Pair<Coordinate, String> parsePlayNotation(final String notation) {
		final Matcher m = PLAY_TILES.matcher(notation);
		if (!m.matches()) {
			throw new AssertionError();
		}
		return Pair.of(Coordinate.parse(m.group(1)), m.group(2));
	}

	@Override
	public String toString() {
		return "Action{" +
				"notation='" + this.notation + '\'' +
				'}';
	}

	public static class SkipTurn extends Action {
		public SkipTurn(final UUID player, final String notation) {
			super(player, notation);
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
		private PlayTiles(final UUID player, String notation) {
			super(player, notation);
			final Pair<Coordinate, String> parsed = parsePlayNotation(notation);
			this.startSquare = parsed.getLeft();
			this.word = parsed.getRight();
		}

		public Grid.Direction getDirection() {
			return this.startSquare.direction;
		}
	}
}
