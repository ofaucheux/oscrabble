package oscrabble.data.objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.ScrabbleException;
import oscrabble.data.Square;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Grid
{
	public static final Logger LOGGER = LoggerFactory.getLogger(Grid.class);

	private final Square[][] squares;
	private final int size;

	public Grid(final int gridSize)
	{
		this.size = gridSize;

		this.squares = new Square[this.size +2][];
		for (int x = 0; x < this.squares.length; x++)
		{
			this.squares[x] = new Square[this.size + 2];
			for (int y = 0; y < this.squares.length; y++)
			{
				this.squares[x][y] = new Square(x, y);
			}
		}
	}

	public Square get(final Coordinate coordinate)
	{
		return get(coordinate.x, coordinate.y);
	}

	public Square get(int x, int y)
	{
		return this.squares[x][y];
	}

	/**
	 * @return if the complete grid is empty
	 */
	public boolean isEmpty()
	{
		for (final Square[] lines : this.squares)
		{
			for (final Square square : lines)
			{
				if (square.c != null)
				{
					return false;
				}
			}
		}
		return true;
	}

	public boolean isEmpty(final String coordinate) throws ScrabbleException.ForbiddenPlayException
	{
		final Coordinate triple = getCoordinate(coordinate);
		return this.get(triple).c == null;
	}

	public Character getChar(final String notation) throws ScrabbleException.ForbiddenPlayException
	{
		final Coordinate coordinate = getCoordinate(notation);
		return this.get(coordinate).c;
	}

	public Collection<Square> getAllSquares()
	{
		final ArrayList<Square> list = new ArrayList<>();
		for (final Square[] line : this.squares)
		{
			for (final Square square : line)
			{
				list.add(square);
			}
		}
		return list;
	}

	public int getSize()
	{
		return this.size;
	}

	/**
	 * A square
	 */
	public class Square
	{
		public final int x;
		public final int y;
		public int letterBonus;
		public int wordBonus;

//		/**
//		 * Action, which has filled this field.
//		 */
//		public Action action;

		public Character c;

		private Square(final int x, final int y)
		{
			this.x = x;
			this.y = y;

			final Bonus bonus = calculateBonus(x, y);
			this.wordBonus = bonus.wordFactor;
			this.letterBonus = bonus.charFactor;
		}

		public boolean isEmpty()
		{
			return this.c == null;
		}

		public Square getNeighbour(final Direction direction, int value)
		{
			return Grid.this.get(
					this.x + (direction == Direction.HORIZONTAL ? value : 0),
					this.y + (direction == Direction.VERTICAL ? value : 0)
			);
		}

		public boolean isBorder()
		{
			return this.x == 0 || this.x == Grid.this.size + 1
					|| this.y == 0 || this.y == Grid.this.size + 1;
		}

		public Set<Square> getNeighbours()
		{
			final Set<Square> neighbours = new HashSet<>(4);
			for (final Direction dir : Direction.values())
			{
				if (!isFirstOfLine(dir))
				{
					neighbours.add(getPrevious(dir));
				}
				if (!isLastOfLine(dir))
				{
					neighbours.add(getNext(dir));
				}
			}
			return neighbours;
		}

		public boolean isFirstOfLine(final Direction direction)
		{
			final int position = direction == Direction.HORIZONTAL ? this.x : this.y;
			return position == 0;
		}

		public Square getPrevious(final Direction direction)
		{
			return getNeighbour(direction, -1);
		}

		public Square getNext(final Direction direction)
		{
			return getNeighbour(direction, +1);
		}

		public boolean isLastOfLine(final Direction direction)
		{
			final int position = direction == Direction.HORIZONTAL ? this.x : this.y;
			return position == size - 1;
		}

		oscrabble.data.Square toData()
		{
			final oscrabble.data.Square square = new oscrabble.data.Square();
			square.tile = this.c;
			square.x =this.x;
			square.y = this.y;
			return square;
		}
	}

	private static final Pattern VERTICAL_COORDINATE_PATTERN = Pattern.compile("(\\d+)(\\w)");
	private static final Pattern HORIZONTAL_COORDINATE_PATTERN = Pattern.compile("(\\w)(\\d+)");

	public static Coordinate getCoordinate(final String notation) throws ScrabbleException.ForbiddenPlayException
	{
		final Direction direction;
		final int groupX, groupY;
		Matcher m;
		if ((m = HORIZONTAL_COORDINATE_PATTERN.matcher(notation)).matches())
		{
			direction = Direction.HORIZONTAL;
			groupX = 2;
			groupY = 1;
		}
		else if ((m = VERTICAL_COORDINATE_PATTERN.matcher(notation)).matches())
		{
			direction = Direction.VERTICAL;
			groupX = 1;
			groupY = 2;
		}
		else
		{
			throw new ScrabbleException.ForbiddenPlayException("Cannot parse coordinate: " + notation);
		}

		final Coordinate c = new Coordinate();
		c.direction = direction;
		c.x = Integer.parseInt(m.group(groupX));
		c.y = m.group(groupY).charAt(0) - 'A' + 1;
		return c;
	}

	/**
	 * Liefert den Bonus einer Zelle.
	 * @param x {@code 0} for border
	 */
	private Bonus calculateBonus(final int x, final int y)
	{

		final int midColumn = this.size / 2 + 1;

		if (x > midColumn)
		{
			return calculateBonus(this.size - x +1, y);
		}
		else if (y > midColumn)
		{
			return calculateBonus(x, this.size - y + 1);
		}

		if (x > y)
		{
			//noinspection SuspiciousNameCombination
			return calculateBonus(y, x);
		}

		if (x == 0 || y == 0)
		{
			return Bonus.BORDER;
		}

//		if (this.size != SCRABBLE_SIZE)
//		{
//			return Bonus.NONE;
//		}
//
		assert (1 <= x && x <= y && y <= midColumn);

		if (x == y)
		{
			switch (x)
			{
				case 1:
					return Bonus.RED;
				case 6:
					return Bonus.DARK_BLUE;
				case 7:
					return Bonus.LIGHT_BLUE;
				default:
					return Bonus.ROSE;
			}
		}
		else if (x == 1 && y == midColumn)
		{
			return Bonus.RED;
		}
		else if (x == 1 && y == 4)
		{
			return Bonus.LIGHT_BLUE;
		}
		else if (x == 2 && y == 6)
		{
			return Bonus.DARK_BLUE;
		}
		else if (x == 3 && y == 7)
		{
			return Bonus.LIGHT_BLUE;
		}
		else if (x == 4 && y == 8)
		{
			return Bonus.LIGHT_BLUE;
		}
		else
		{
			return Bonus.NONE;
		}
	}

	public oscrabble.data.Grid toData()
	{
		final oscrabble.data.Grid grid = new oscrabble.data.Grid();
		grid.squares = new ArrayList<>();
		for (final Square[] line : this.squares)
		{
			for (final Square square : line)
			{
				grid.squares.add(square.toData());
			}
		}
		return grid;
	}

	/**
	 * Definition der Bonus-Zellen.
	 */
	public enum Bonus
	{
		BORDER, NONE, RED(3, 1), ROSE(2, 1), DARK_BLUE(1, 3), LIGHT_BLUE(1, 2);

		private final int wordFactor;
		private final int charFactor;

		Bonus()
		{
			this(1, 1);
		}

		Bonus(int wordFactor, int charFactor)
		{
			this.wordFactor = wordFactor;
			this.charFactor = charFactor;
		}
	}

	/**
	 * Get the column nummer matching a letter, as in A8.
	 */
	public enum Direction
	{
		HORIZONTAL, VERTICAL;
		public Direction other()
		{
			return (this == HORIZONTAL ? VERTICAL : HORIZONTAL);
		}
	}

	public static class Coordinate
	{
		public Direction direction;
		public int x, y;

		public String getNotation()
		{
			return getNotation(this.x, this.y, this.direction);
		}

		public static String getNotation(final Grid.Square square, final Direction direction)
		{
			return getNotation(square.x, square.y, direction);
		}

		private static String getNotation(final int x, final int y, final Direction direction)
		{
			String sy = Character.toString((char) ('A' + y));
			switch (direction)
			{
				case HORIZONTAL:
					return sy + x;
				case VERTICAL:
					return x + sy;
				default:
					throw new AssertionError();
			}
		}
	}
}


