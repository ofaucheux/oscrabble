package oscrabble.server.action;

import oscrabble.ScrabbleException;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Action
{
	public UUID id;
	public String notation;

	private static final Pattern PASS_TURN = Pattern.compile("-");
	private static final Pattern EXCHANGE = Pattern.compile("-\\s+(\\S+)");
	private static final Pattern HORIZONTAL_COORDINATE_PATTERN = Pattern.compile("((\\d+)(\\w))(\\s+(\\S*))?");
	private static final Pattern VERTICAL_COORDINATE_PATTERN = Pattern.compile("((\\w)(\\d+))(\\s+(\\S*))?");

	static public Action parse(oscrabble.data.Action jsonAction) throws ScrabbleException.ForbiddenPlayException
	{
		final String not = jsonAction.notation;
		final Action action = parse(not);
		action.id = jsonAction.playID;
		return action;
	}

	public static Action parse(final String notation) throws ScrabbleException.ForbiddenPlayException
	{
		final Action action;
		if (PASS_TURN.matcher(notation).matches())
		{
			action = new SkipTurn();
		}
		else if (EXCHANGE.matcher(notation).matches())
		{
			action = new Exchange(notation);
		}
		else if (HORIZONTAL_COORDINATE_PATTERN.matcher(notation).matches()
				|| VERTICAL_COORDINATE_PATTERN.matcher(notation).matches())
		{
			action = new PlayTiles(notation);
		}
		else
		{
			throw new ScrabbleException.ForbiddenPlayException("Illegal move notation: \"" + notation + "\"");
		}

		action.notation = notation;
		return action;
	}

	public static class SkipTurn extends Action
	{
	}

	/**
	 * Action für den Austausch von Buchstaben.
	 */
	public static class Exchange extends Action
	{

		public final char[] toExchange;

		public Exchange(final String notation)
		{
			final String chars = EXCHANGE.matcher(notation).group(1);
			this.toExchange = chars.toCharArray();
		}
	}

	public static class PlayTiles extends Action
	{

		public final Direction direction;

		/**
		 * The word created by this move, incl. already set tiles and where blanks are represented by their value letters.
		 */
		public String word;
		public final int x;
		public final int y;

		/**
		 * Die Blanks (mindesten neugespielt) werden durch klein-buchstaben dargestellt.
		 */
		public PlayTiles(String notation)
		{
			Matcher matcher;
			int groupX;
			int groupY;
			if ((matcher = HORIZONTAL_COORDINATE_PATTERN.matcher(notation)).matches())
			{
				this.direction = Direction.HORIZONTAL;
				groupX = 3;
				groupY = 2;
			}
			else if ((matcher = VERTICAL_COORDINATE_PATTERN.matcher(notation)).matches())
			{
				this.direction = Direction.VERTICAL;
				groupX = 2;
				groupY = 3;
			}
			else
			{
				throw new AssertionError("Cannot parse: " + notation);
			}
			this.x = (Character.toUpperCase(matcher.group(groupX).charAt(0)) - 'A' + 1);
			this.y = Integer.parseInt(matcher.group(groupY));
			this.word = matcher.group(5);
		}
	}

	public enum Direction {HORIZONTAL, VERTICAL;

		public Direction other()
		{
			return (this == HORIZONTAL ? VERTICAL : HORIZONTAL);
		}
	}
}
