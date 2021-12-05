package oscrabble.controller;

import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;
import oscrabble.ScrabbleException;
import oscrabble.data.objects.Coordinate;
import oscrabble.data.objects.Grid;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Action {

	private static final Pattern SET_REFUSED_WORD_LIST = Pattern.compile("REFUSE:(\\s*)");

	private static final Pattern PASS_TURN = Pattern.compile(
			Pattern.quote(oscrabble.data.Action.PASS_TURN_NOTATION)
	);
	private static final Pattern EXCHANGE = Pattern.compile("-\\s+(\\S+)");
	private static final Pattern PLAY_TILES = Pattern.compile("(\\S*)\\s+(\\S*)");
	public static UUID NOT_A_PLAYER_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

	public UUID turnId;
	public final UUID player;

	public Integer score;

	protected Action(final UUID player) {
		this.player = player;
	}

	static public Action parse(oscrabble.data.Action jsonAction) throws ScrabbleException.NotParsableException {
		final Action action = parse(jsonAction.player, jsonAction.notation);
		action.turnId = jsonAction.turnId;
		return action;
	}

	public static Action parse(final UUID player, final String notation) throws ScrabbleException.NotParsableException {
		final Action action;
		Matcher matcher;
		if (PASS_TURN.matcher(notation).matches()) {
			action = new SkipTurn(player);
		} else if (EXCHANGE.matcher(notation).matches()) {
			action = new Exchange(player, notation);
		} else if ((matcher = SET_REFUSED_WORD_LIST.matcher(notation)).matches()) {
			action = new SetRefusedWordAction(matcher.group(1));
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
				"notation='" + this.getNotation() + '\'' +
				'}';
	}

	/**
	 * @return standardized scrabble notation for this action
	 */
	public abstract String getNotation();

	public static class SkipTurn extends Action {

		public SkipTurn(final UUID player) {
			super(player);
		}

		@Override
		public String getNotation() {
			return oscrabble.data.Action.PASS_TURN_NOTATION;
		}
	}

	/**
	 * Action für den Austausch von Buchstaben.
	 */
	public static class Exchange extends Action {

		public final char[] toExchange;

		@Getter
		private final String notation;

		private Exchange(final UUID player, final String notation) {
			super(player);
			this.notation = notation;
			final String chars = EXCHANGE.matcher(notation).group(1);
			this.toExchange = chars.toCharArray();
		}
	}

	public static class PlayTiles extends Action {

		public final Coordinate startSquare;

		@Getter
		private final String notation;

		/**
		 * The word created by this move, incl. already set tiles and where blanks are represented by their value letters.
		 */
		public String word;

		/**
		 * Die Blanks (mindesten neugespielt) werden durch klein-buchstaben dargestellt.
		 */
		private PlayTiles(final UUID player, String notation) {
			super(player);
			this.notation = notation;
			final Pair<Coordinate, String> parsed = parsePlayNotation(notation);
			this.startSquare = parsed.getLeft();
			this.word = parsed.getRight();
		}

		public Grid.Direction getDirection() {
			return this.startSquare.direction;
		}
	}
}
