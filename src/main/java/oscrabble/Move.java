package oscrabble;

import oscrabble.dictionary.Dictionary;
import oscrabble.server.IAction;

import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Move implements IAction
{
	public final Grid.Square startSquare;
	final Direction direction;

	/** The word created by this move, incl. already set tiles and where blanks are represented by their value letters. */
	public String word;
	private final String originalWord;

	/**
	 * Die Blanks (mindesten neugespielt) werden durch klein-buchstaben dargestellt.
	 */
	public Move(final Grid.Square startSquare,
				final Direction direction,
				final String word)
	{
		this.startSquare = startSquare;
		this.direction = direction;
		this.originalWord = word;
		this.word = word.toUpperCase();
	}

	private static final Pattern HORIZONTAL_COORDINATE_PATTERN = Pattern.compile("(\\d+)(\\w)\\s+(\\S*)");
	private static final Pattern VERTICAL_COORDINATE_PATTERN = Pattern.compile("(\\w)(\\d+)\\s+(\\S*)");

	/**
	 * @param description
	 * @return {@code true} wenn die Zeichenkette tatsächlich einen Spielzug beschreibt.
	 */
	public static boolean isMoveDescription(final String description)
	{
		return
				HORIZONTAL_COORDINATE_PATTERN.matcher(description).matches()
				|| VERTICAL_COORDINATE_PATTERN.matcher(description).matches();
	}

	public LinkedHashMap<Grid.Square, Stone> getStones(final Grid grid, final Dictionary dictionary)
	{
		final LinkedHashMap<Grid.Square, Stone> stones = new LinkedHashMap<>();
		int x = this.startSquare.getX();
		int y = this.startSquare.getY();
		for (int i = 0; i < this.word.length(); i++)
		{
			char c = this.originalWord.charAt(i);
			stones.put(grid.getSquare(x, y), dictionary.generateStone(Character.isLowerCase(c) ? null : c));
			if (this.direction == Direction.HORIZONTAL)
			{
				x += 1;
			}
			else
			{
				y += 1;
			}
		}

		return stones;
	}

	/**
	 *
	 * @return Liste der belegten Zellen.
	 */
	public	LinkedHashMap<Grid.Square, Character> getSquares()
	{
		final LinkedHashMap<Grid.Square, Character> squares = new LinkedHashMap<>(this.word.length());
		Grid.Square square = this.startSquare;
		for (int i = 0; i < this.word.length(); i++)
		{
			squares.put(square, this.word.charAt(i));
			if (i != this.word.length() - 1)
			{
				square = square.getFollowing(this.direction);
			}
		}
		return squares;
	}

	/**
	 * Parse die Beschreibung eines Spielzuges.
	 *
	 * @param grid          die
	 * @param coordinate    die Beschreibung, z.B. {@code B4 WAGEN} für Honizontal, B, 4 Wort WAGEN.
	 * @return der Spielzug
	 * @throws ParseException wenn aus der Beschreibung keinen Spielzug zu finden ist.
	 */
	public static Move parseMove(final Grid grid, final String coordinate) throws ParseException
	{
		final Move.Direction direction;
		final int groupX;
		final int groupY;
		Matcher matcher;
		if ((matcher = HORIZONTAL_COORDINATE_PATTERN.matcher(coordinate)).matches())
		{
			direction = Move.Direction.HORIZONTAL;
			groupX = 2;
			groupY = 1;
		}
		else if ((matcher = VERTICAL_COORDINATE_PATTERN.matcher(coordinate)).matches())
		{
			direction = Move.Direction.VERTICAL;
			groupX = 1;
			groupY = 2;
		}
		else
		{
			throw new ParseException(coordinate, 0);
		}
		final int x = (Character.toUpperCase(matcher.group(groupX).charAt(0)) - 'A' + 1);
		final int y = Integer.parseInt(matcher.group(groupY));
		final Move move = new Move(grid.getSquare(x, y), direction, matcher.group(3));
		return move;
	}

	public Direction getDirection()
	{
		return this.direction;
	}

	public boolean isPlayedByBlank(final int i)
	{
		return Character.isLowerCase(this.originalWord.charAt(i));
	}

	public enum Direction
	{
		VERTICAL, HORIZONTAL;

		public Direction other()
		{
			return this == VERTICAL ? HORIZONTAL : VERTICAL;
		}
	}

	@Override
	public int hashCode()
	{
		return this.originalWord.hashCode();
	}

	@Override
	public boolean equals(final Object other)
	{
		if (!(other instanceof Move))
		{
			return false;
		}
		return this.startSquare.equals(((Move) other).startSquare)
				&& this.direction == ((Move) other).direction
				&& this.originalWord.equals(((Move) other).originalWord);
	}

	@Override
	public String toString()
	{
		final String column = Character.toString((char) ('A' - 1 + this.startSquare.getX()));
		final String line = Integer.toString(this.startSquare.getY());
		return String.format("%3s  %s",
				(this.direction == Direction.HORIZONTAL) ? line + column : column + line,
				this.originalWord
		);
	}

	/**
	 * Sort a list of move
	 * @param list  list to sort
	 * @param grid  current grid
	 * @param comparator comparator on {@link oscrabble.Grid.MoveMetaInformation}
	 * @throws ScrabbleException
	 */
	public static void sort(final List<Move> list, final Grid grid, final Comparator<Grid.MoveMetaInformation> comparator) throws ScrabbleException
	{
		final HashMap<Move, Grid.MoveMetaInformation> mapping = new HashMap<>();
		for (Move w : list)
		{
			mapping.put(w, grid.getMetaInformation(w));
		}
		final Comparator<Move> moveComparator = new Comparator<Move>()
		{
			@Override
			public int compare(final Move o1, final Move o2)
			{
				return comparator.compare(mapping.get(o1), mapping.get(o2));
			}
		};
		list.sort(moveComparator);
	}
}
