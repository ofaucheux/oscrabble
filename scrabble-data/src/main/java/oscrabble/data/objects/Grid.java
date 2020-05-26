package oscrabble.data.objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.ScrabbleException;
import oscrabble.controller.Action;
import oscrabble.data.ScrabbleRules;
import oscrabble.data.Tile;

import java.util.*;

public class Grid
{
	public static final Logger LOGGER = LoggerFactory.getLogger(Grid.class);
	public static final int GRID_SIZE = 15;
	public static final int GRID_SIZE_PLUS_2 = GRID_SIZE + 2;

	/**
	 * Arrays the cases, as the first and last are border.
	 */
	private final oscrabble.data.objects.Square[] squares;

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
		this.squares = new oscrabble.data.objects.Square[(GRID_SIZE + 2) * (GRID_SIZE + 2)];
		if (fillGrid)
		{
			fillGrid(true);
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
			final Coordinate coordinate = Coordinate.parse(sq.coordinate);
			g.squares[coordinate.x * GRID_SIZE_PLUS_2 + coordinate.y] = oscrabble.data.objects.Square.fromData(sq);
		}
		return g;
	}

	/**
	 * Fill the grid with empty squares
	 */
	void fillGrid(boolean inclusiveInside)
	{
		for (int i = 0; i < GRID_SIZE_PLUS_2 * GRID_SIZE_PLUS_2; i++)
		{
			final int x = i / GRID_SIZE_PLUS_2;
			final int y = i % GRID_SIZE_PLUS_2;
			boolean border = i < GRID_SIZE_PLUS_2;
			border |= i >= GRID_SIZE_PLUS_2 * (GRID_SIZE + 1);
			border |= y == 0 || y == (GRID_SIZE + 1);
			if (inclusiveInside || border)
			{
				this.squares[i] = new oscrabble.data.objects.Square(x, y);
			}
		}
	}

	public oscrabble.data.objects.Square get(final Coordinate coordinate)
	{
		return get(coordinate.x, coordinate.y);
	}

	public oscrabble.data.objects.Square get(final String coordinate)
	{
		final Coordinate c = Coordinate.parse(coordinate);
		return get(c);
	}

	/**
	 * (1-based coordinates: Borders are 0, first square is 1,1)
	 * @param x x
	 * @param y y
	 * @return the square
	 */
	public oscrabble.data.objects.Square get(int x, int y)
	{
		return this.squares[x * GRID_SIZE_PLUS_2 + y];
	}

	/**
	 * @return if the complete grid is empty
	 */
	public boolean isEmpty()
	{
		for (final oscrabble.data.objects.Square square : this.squares)
		{
			if (square.tile != null)
			{
				return false;
			}
		}
		return true;
	}

	public boolean isEmpty(final String coordinate)
	{
		final Coordinate triple = Coordinate.parse(coordinate);
		return this.get(triple).tile == null;
	}

	public Collection<oscrabble.data.objects.Square> getAllSquares()
	{
		return Arrays.asList(this.squares);
	}

	public int getSize()
	{
		return GRID_SIZE;
	}


	public void play(final ScrabbleRules rules, final String move) throws ScrabbleException.NotParsableException
	{
		play(rules, (Action.PlayTiles) Action.parse(null, move));
	}

	public void play(final ScrabbleRules rules, final Action.PlayTiles playTiles)
	{
		oscrabble.data.objects.Square sq = get(playTiles.startSquare);
		for (int i = 0; i < playTiles.word.length(); i++)
		{
			final char c = playTiles.word.charAt(i);
			final char uppercase = Character.toUpperCase(c);
			if (sq.isEmpty())
			{
				final boolean isJoker = Character.isLowerCase(c);
				sq.tile = Tile.builder()
						.isJoker(isJoker)
						.c(uppercase)
						.points(
								isJoker || rules == null
										? 0
										: rules.getLetters().get(uppercase).points)
						.position(sq.getCoordinate())
						.turn(playTiles.turnId)
						.build();
			}
			else if (Character.toUpperCase(sq.tile.c) != uppercase)
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

	/**
	 * Construct from grid data
	 * @param data grid data - null for test purposes
	 */
	public Grid(final oscrabble.data.Grid data)
	{
		this(false);
		for (final oscrabble.data.Square dataSq : data.squares)
		{
			final Square sq = new Square(dataSq);
			this.squares[sq.x * GRID_SIZE_PLUS_2 + sq.y] = sq;
		}

		// fill the borders
		for (int i = 0; i < this.squares.length; i++)
		{
			if (this.squares[i]==null)
			{
				this.squares[i] = new Square(i / GRID_SIZE_PLUS_2, i % GRID_SIZE_PLUS_2);
			}
		}

	}

	public oscrabble.data.objects.Square getCentralSquare()
	{
		final int center = this.squares.length / 2;
		return this.squares[center];
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
		for (final oscrabble.data.objects.Square square : this.squares)
		{
			sb.append(square.isBorder ? '#' : square.tile == null ? ' ' : square.tile.c);
			if (square.y == GRID_SIZE + 1)
			{
				sb.append('\n');
			}
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

}


