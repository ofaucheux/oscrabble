package oscrabble.data.objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.ScrabbleException;
import oscrabble.controller.Action;
import oscrabble.exception.IllegalCoordinate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Grid
{
	public static final Logger LOGGER = LoggerFactory.getLogger(Grid.class);
	public static final int GRID_SIZE = 15;

	/**
	 * Arrays with 17 cases, as the first and last are border.
	 */
	private final oscrabble.data.objects.Square[][] squares;

	/**
	 * Create a grid, inclusive its squares.
	 */
	public Grid()
	{
		this(true);
	}

	/**
	 * Create a grid.
	 * @param fillGrid are the intern squares to be created too?
	 */
	private Grid(final boolean fillGrid)
	{
		this.squares = new oscrabble.data.objects.Square[GRID_SIZE + 2][];
		if (fillGrid)
		{
			fillGrid(true);
		}
	}

	/**
	 * Fill the grid with empty squares
	 */
	void fillGrid(boolean inclusiveInside)
	{
		for (int x = 0; x < this.squares.length; x++)
		{
			this.squares[x] = new oscrabble.data.objects.Square[GRID_SIZE + 2];
			for (int y = 0; y < this.squares.length; y++)
			{
				if (inclusiveInside || x == 0 || y == 0 || x == GRID_SIZE + 1 || y == GRID_SIZE + 1)
				{
					this.squares[x][y] = new oscrabble.data.objects.Square(x, y);
				}
			}
		}
	}

	/**
	 * Create a grid from a data object.
	 *
	 * @param data -
	 * @return the new grid
	 */
	public static Grid fromData(final oscrabble.data.Grid data)
	{
		final Grid g = new Grid(false);
		g.fillGrid(false);
		for (final oscrabble.data.Square sq : data.squares)
		{
			final Coordinate coordinate = getCoordinate(sq.coordinate);
			g.squares[coordinate.x][coordinate.y] = oscrabble.data.objects.Square.fromData(sq);
		}
		return g;
	}

	public oscrabble.data.objects.Square get(final Coordinate coordinate)
	{
		return get(coordinate.x, coordinate.y);
	}

	public oscrabble.data.objects.Square get(final String coordinate)
	{
		final Coordinate c = getCoordinate(coordinate);
		return get(c);
	}

	/**
	 * (0-based coordinates)
	 * @param x x
	 * @param y y
	 * @return the square
	 */
	public oscrabble.data.objects.Square get(int x, int y)
	{
		return this.squares[x][y];
	}

	/**
	 * @return if the complete grid is empty
	 */
	public boolean isEmpty()
	{
		for (final oscrabble.data.objects.Square[] lines : this.squares)
		{
			for (final oscrabble.data.objects.Square square : lines)
			{
				if (square.c != null)
				{
					return false;
				}
			}
		}
		return true;
	}

	public boolean isEmpty(final String coordinate)
	{
		final Coordinate triple = getCoordinate(coordinate);
		return this.get(triple).c == null;
	}

	public Collection<oscrabble.data.objects.Square> getAllSquares()
	{
		final ArrayList<oscrabble.data.objects.Square> list = new ArrayList<>();
		for (final oscrabble.data.objects.Square[] line : this.squares)
		{
			Collections.addAll(list, line);
		}
		return list;
	}

	public int getSize()
	{
		return GRID_SIZE;
	}


	public void play(final String move) throws ScrabbleException.ForbiddenPlayException
	{
		play((Action.PlayTiles) Action.parse(move));
	}

	public void play(final Action.PlayTiles playTiles)
	{
		oscrabble.data.objects.Square sq = get(playTiles.startSquare);
		for (int i = 0; i < playTiles.word.length(); i++)
		{
			final char c = playTiles.word.charAt(i);
			if (sq.isEmpty())
			{
				sq.c = c;
			}
			else if (Character.toUpperCase(sq.c) != Character.toUpperCase(c))
			{
				throw new AssertionError("The case is already occupied");
			}

			sq = getNeighbour(sq, playTiles.startSquare.direction, 1);
		}
	}

	/**
	 * @param coordinate coordinate of square
	 * @return the words which the square is part of.
	 */
	public Set<String> getWords(final String coordinate)
	{
		final oscrabble.data.objects.Square origin = get(coordinate);
		if (origin.isEmpty())
		{
			return Collections.emptySet();
		}

		final LinkedHashSet<String> words = new LinkedHashSet<>();
		for (final Direction dir : Direction.values())
		{
			final StringBuilder sb = new StringBuilder();
			oscrabble.data.objects.Square sq = origin;
			while (!(sq = getPrevious(sq, dir)).isBorder() && !sq.isEmpty())
			{
				sb.insert(0, sq.getLetter());
			}
			sb.append((sq = origin).getLetter());
			while (!(sq = getNext(sq, dir)).isBorder() && !sq.isEmpty())
			{
				sb.append(sq.getLetter());
			}
			if (sb.length() > 1)
			{
				words.add(sb.toString());
			}
		}
		return words;
	}

	private static final Pattern HORIZONTAL_COORDINATE_PATTERN = Pattern.compile("(\\w)(\\d+)");
	private static final Pattern VERTICAL_COORDINATE_PATTERN = Pattern.compile("(\\d+)(\\w)");

	public static Coordinate getCoordinate(final String notation) throws IllegalCoordinate
	{
		final Direction direction;
		final int groupX, groupY;
		Matcher m;
		if ((m = HORIZONTAL_COORDINATE_PATTERN.matcher(notation)).matches()) // horizontal ist zuerst x, heißt: Buchstabe.
		{
			direction = Direction.HORIZONTAL;
			groupX = 1;
			groupY = 2;
		}
		else if ((m = VERTICAL_COORDINATE_PATTERN.matcher(notation)).matches())
		{
			direction = Direction.VERTICAL;
			groupX = 2;
			groupY = 1;
		}
		else
		{
			throw new IllegalCoordinate(notation);
		}

		final Coordinate c = new Coordinate();
		c.direction = direction;
		c.x = m.group(groupX).charAt(0) - 'A' + 1;
		c.y = Integer.parseInt(m.group(groupY));
		return c;
	}

	public oscrabble.data.objects.Square getCentralSquare()
	{
		final int center = (int) Math.ceil(GRID_SIZE / 2f);
		return this.squares[center][center];
	}


	public oscrabble.data.objects.Square getNeighbour(final oscrabble.data.objects.Square sq, final Direction direction, int value)
	{
		return Grid.this.get(
				sq.x + (direction == Direction.HORIZONTAL ? value : 0),
				sq.y + (direction == Direction.VERTICAL ? value : 0)
		);
	}


	public Set<oscrabble.data.objects.Square> getNeighbours(final oscrabble.data.objects.Square sq)
	{
		final Set<oscrabble.data.objects.Square> neighbours = new HashSet<>(4);
		for (final Direction dir : Direction.values())
		{
			if (!sq.isFirstOfLine(dir))
			{
				neighbours.add(getPrevious(sq, dir));
			}
			if (!sq.isLastOfLine(dir))
			{
				neighbours.add(getNext(sq, dir));
			}
		}
		return neighbours;
	}

	public oscrabble.data.objects.Square getPrevious(final oscrabble.data.objects.Square sq, final Direction direction)
	{
		return getNeighbour(sq, direction, -1);
	}

	public oscrabble.data.objects.Square getNext(final oscrabble.data.objects.Square sq, final Direction direction)
	{
		return getNeighbour(sq, direction, +1);
	}

	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder("Grid{\n");
		for (final oscrabble.data.objects.Square[] line : this.squares)
		{
			for (final oscrabble.data.objects.Square square : line)
			{
				sb.append(square.isBorder ? '#' : square.c == null ? ' ' : square.c);
			}
			sb.append('\n');
		}
		sb.append("}");
		return sb.toString();
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

		// tODO: move
		public static String getNotation(final oscrabble.data.objects.Square square, final Direction direction)
		{
			return getNotation(square.x, square.y, direction);
		}

		/**
		 * The coordinates are 0-based.
		 *
		 * @param x
		 * @param y
		 * @param direction
		 * @return
		 */
		private static String getNotation(final int x, final int y, final Direction direction)
		{
			String sx = Character.toString((char) ('A' + x-1));
			switch (direction)
			{
				case HORIZONTAL:
 					return sx + (y);
				case VERTICAL:
					return y + sx;
				default:
					throw new AssertionError();
			}
		}
	}
}


