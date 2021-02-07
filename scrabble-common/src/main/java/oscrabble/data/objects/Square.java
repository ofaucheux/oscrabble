package oscrabble.data.objects;

import oscrabble.data.Tile;

import java.util.Objects;
import java.util.UUID;

/**
 * A square (possibly a border one) with coordinate, contained tile and bonus.
 */
public class Square
{
	/**
	 * Values are 1-based: the case A1 has the coordinate (1,1). The case (1,0) exists, but is marked as border one.
	 */
	final int x;
	final int y;

	public final boolean isBorder;

	public int letterBonus;

	public int wordBonus;


	/**
	 * Action, which has filled this field.
	 */
	public UUID action;

	public Tile tile;

	Square(final int x, final int y)
	{
		this.x = x;
		this.y = y;
		this.isBorder = x == 0 || x == Grid.GRID_SIZE + 1
				|| y == 0 || y == Grid.GRID_SIZE + 1;

		final Bonus bonus = calculateBonus(x, y);
		this.wordBonus = bonus.wordFactor;
		this.letterBonus = bonus.charFactor;
	}

	/**
	 * Construct from a data object
	 * @param dataSq data square
	 */
	public Square(final oscrabble.data.Square dataSq)
	{
		final Coordinate c = Coordinate.parse(dataSq.coordinate);
		this.x = c.x;
		this.y = c.y;
		this.tile = dataSq.tile;
		this.action = dataSq.settingPlay;
		this.letterBonus = dataSq.letterBonus;
		this.wordBonus = dataSq.wordBonus;
		this.isBorder = false;

		assert (this.x > 0 && this.x <= Grid.GRID_SIZE && this.y > 0 && this.y <= Grid.GRID_SIZE);
	}

	/**
	 * Create a square from a data object.
	 * @param data source
	 * @return created square
	 */
	public static Square fromData(final oscrabble.data.Square data)
	{
		final Coordinate c = Coordinate.parse(data.coordinate);
		if ((c.x < 1 || c.y < 1))
		{
			throw new AssertionError();
		}
		final Square sq = new Square(c.x, c.y);
		sq.action = data.settingPlay;
		sq.letterBonus = data.letterBonus;
		sq.wordBonus = data.wordBonus;
		sq.tile = data.tile;
		return sq;
	}

	/**
	 * Remark: a border square is empty.
	 * @return if empty or not.
	 */
	public boolean isEmpty()
	{
		return this.tile == null;
	}

	public boolean isBorder()
	{
		return this.x == 0 || this.x == Grid.GRID_SIZE + 1
				|| this.y == 0 || this.y == Grid.GRID_SIZE + 1;
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public boolean isFirstOfLine(final Grid.Direction direction)
	{
		final int position = direction == Grid.Direction.HORIZONTAL ? this.x : this.y;
		return position == 1;
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public boolean isLastOfLine(final Grid.Direction direction)
	{
		final int position = direction == Grid.Direction.HORIZONTAL ? this.x : this.y;
		return position == Grid.GRID_SIZE;
	}

	oscrabble.data.Square toData()
	{
		final oscrabble.data.Square square = oscrabble.data.Square.builder()
				.tile(this.tile)
				.coordinate(this.getCoordinate())
				.letterBonus(this.letterBonus)
				.wordBonus(this.wordBonus)
				.settingPlay(this.action)
				.build();
		return square;
	}

	@Override
	public String toString()
	{
		return isBorder ? "#" : getNotation(Grid.Direction.HORIZONTAL);
	}

	public String getNotation(final Grid.Direction direction)
	{
		return Coordinate.getNotation(this, direction);
	}

	/**
	 *
	 * @return the coordinate of the square in form "A1"
	 */
	public String getCoordinate()
	{
		return Coordinate.getNotation(this, Grid.Direction.HORIZONTAL);
	}

	/**
	 *
	 * @return x-coordinate, 1-based
	 */
	public int getX()
	{
		return this.x;
	}

	/**
	 *
	 * @return y-coordinate, 1-based.
	 */
	public int getY()
	{
		return this.y;
	}
//
//	public boolean isJoker()
//	{
//		return this.tile != null && Character.isLowerCase(this.tile);
//	}

	@Override
	public boolean equals(final Object other)
	{
		if (this == other) return true;
		if (other == null || getClass() != other.getClass()) return false;
		final Square o = (Square) other;
		final boolean equals = this.x == o.x
				&& this.y == o.y
				&& this.isBorder == o.isBorder
				&& this.letterBonus == o.letterBonus
				&& this.wordBonus == o.wordBonus
				&& Objects.equals(this.tile, o.tile);
		return equals;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(this.x, this.y, this.isBorder, this.letterBonus, this.wordBonus, this.action, this.tile);
	}

	/**
	 *
	 * @return the uppercase character of the stone played on this square, {@code null} if no such.
	 */
	Character getLetter()
	{
		return this.tile == null ? null : Character.toUpperCase(this.tile.c);
	}

	/**
	 * Liefert den Bonus einer Zelle.
	 * @param x {@code 0} for border
	 */
	private static Bonus calculateBonus(final int x, final int y)
	{

		final int midColumn = Grid.GRID_SIZE / 2 + 1;

		if (x > midColumn)
		{
			return calculateBonus(Grid.GRID_SIZE - x +1, y);
		}
		else if (y > midColumn)
		{
			return calculateBonus(x, Grid.GRID_SIZE - y + 1);
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

//		if (GRID_SIZE != SCRABBLE_SIZE)
//		{
//			return Bonus.NONE;
//		}
//
		assert 1 <= x && x <= y;

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

}
