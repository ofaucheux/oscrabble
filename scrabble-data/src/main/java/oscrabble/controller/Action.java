package oscrabble.controller;

import oscrabble.ScrabbleException;
import oscrabble.data.objects.Coordinate;
import oscrabble.data.objects.Grid;
import oscrabble.data.objects.Square;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Action
{
	public UUID turnId;
	public final String notation;

	private static final Pattern PASS_TURN = Pattern.compile("-");
	private static final Pattern EXCHANGE = Pattern.compile("-\\s+(\\S+)");
	private static final Pattern PLAY_TILES = Pattern.compile("(\\S*)\\s+(\\S*)");

	protected Action(final String notation)
	{
		this.notation = notation;
	}

	static public Action parse(oscrabble.data.Action jsonAction) throws ScrabbleException.ForbiddenPlayException
	{
		final String not = jsonAction.notation;
		final Action action = parse(not);
		action.turnId = jsonAction.turnId;
		return action;
	}

	public static Action parse(final String notation) throws ScrabbleException.ForbiddenPlayException
	{
		final Action action;
		if (PASS_TURN.matcher(notation).matches())
		{
			action = new SkipTurn(notation);
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
		public SkipTurn(final String notation)
		{
			super(notation);
		}
	}

	/**
	 * Action für den Austausch von Buchstaben.
	 */
	public static class Exchange extends Action
	{

		public final char[] toExchange;

		private Exchange(final String notation)
		{
			super(notation);
			final String chars = EXCHANGE.matcher(notation).group(1);
			this.toExchange = chars.toCharArray();
		}
	}

	public static class PlayTiles extends Action
	{

		public final Coordinate startSquare;

		/**
		 * The word created by this move, incl. already set tiles and where blanks are represented by their value letters.
		 */
		public String word;

		/**
		 * Die Blanks (mindesten neugespielt) werden durch klein-buchstaben dargestellt.
		 */
		private PlayTiles(String notation) throws ScrabbleException.ForbiddenPlayException
		{
			super(notation);
			final Matcher m = PLAY_TILES.matcher(notation);
			if (!m.matches())
			{
				throw new AssertionError();
			}

			this.startSquare = Coordinate.parse(m.group(1));
			this.word = m.group(2);
		}

		/**
		 * @return the start square
		 */
		public Square getStartSquare()
		{
			return this.startSquare.getSquare();
		}

		public Grid.Direction getDirection()
		{
			return this.startSquare.direction;
		}
	}
}
