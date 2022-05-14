package oscrabble.data.objects;

import oscrabble.exception.IllegalCoordinate;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Coordinate of a move: cell and direction
 */
public class Coordinate {
	private static final Pattern HORIZONTAL_COORDINATE_PATTERN = Pattern.compile("(\\d+)(\\w)");
	private static final Pattern VERTICAL_COORDINATE_PATTERN = Pattern.compile("(\\w)(\\d+)");

	public Grid.Direction direction;
	public int x, y;

	public Coordinate(char column, int row, Grid.Direction direction) {
		this.x = column - '@';
		this.y = row;
		this.direction = direction;
	}

	public char getColumn() {
		return (char) ('@' + this.x);
	}

	public int getRow() {
		return this.y;
	}

	private Coordinate() {
	}
	public static Coordinate parse(final String notation) throws IllegalCoordinate {
		final Grid.Direction direction;
		final int groupX, groupY;
		Matcher m;
		if ((m = HORIZONTAL_COORDINATE_PATTERN.matcher(notation)).matches()) // horizontal ist zuerst x, heißt: Buchstabe.
		{
			direction = Grid.Direction.HORIZONTAL;
			groupX = 2;
			groupY = 1;
		} else if ((m = VERTICAL_COORDINATE_PATTERN.matcher(notation)).matches()) {
			direction = Grid.Direction.VERTICAL;
			groupX = 1;
			groupY = 2;
		} else {
			throw new IllegalCoordinate(notation);
		}

		final Coordinate c = new Coordinate();
		c.direction = direction;
		c.x = m.group(groupX).charAt(0) - 'A' + 1;
		c.y = Integer.parseInt(m.group(groupY));
		return c;
	}

	// tODO: move
	public static String getNotation(final Square square, final Grid.Direction direction) {
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
	private static String getNotation(final int x, final int y, final Grid.Direction direction) {
		String sx = Character.toString((char) ('A' + x - 1));
		switch (direction) {
			case VERTICAL:
				return sx + (y);
			case HORIZONTAL:
				return y + sx;
			default:
				throw new AssertionError();
		}
	}

	public String getNotation() {
		return getNotation(this.x, this.y, this.direction);
	}

	/**
	 * @return if both objects represent the same cells, perhaps with different directions.
	 */
	public boolean sameCell(final Coordinate other) {
		return this.x == other.x && this.y == other.y;
	}

	/**
	 * @return the square this coordinate points on
	 */
	public Square getSquare() {
		return new Square(this.x, this.y);
	}
}
