package oscrabble.server.action;

import oscrabble.ScrabbleException;
import oscrabble.data.objects.Grid;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Action
{
	public UUID id;
	public String notation;

	private static final Pattern PASS_TURN = Pattern.compile("-");
	private static final Pattern EXCHANGE = Pattern.compile("-\\s+(\\S+)");
	private static final Pattern PLAY_TILES = Pattern.compile("(\\S*)\\s+(\\S*)");

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
		else if (PLAY_TILES.matcher(notation).matches())
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

	@Override
	public String toString()
	{
		return "Action{" +
				"notation='" + this.notation + '\'' +
				'}';
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

		private final Grid.Coordinate coordinate;

		/**
		 * The word created by this move, incl. already set tiles and where blanks are represented by their value letters.
		 */
		public String word;

		/**
		 * Die Blanks (mindesten neugespielt) werden durch klein-buchstaben dargestellt.
		 */
		public PlayTiles(String notation) throws ScrabbleException.ForbiddenPlayException
		{
			final Matcher m = PLAY_TILES.matcher(notation);
			if (!m.matches())
			{
				throw new AssertionError();
			}

			this.coordinate = Grid.getCoordinate(m.group(1));
		}

		public Grid.Direction getDirection()
		{
			try
			{
				return Grid.getCoordinate(notation).direction;
			}
			catch (ScrabbleException.ForbiddenPlayException e)
			{
				throw new Error(e);
			}
		}
	}

	static int getColumn(final char columnLetter)
	{
		return columnLetter - 'A' + 1;
	}
}
