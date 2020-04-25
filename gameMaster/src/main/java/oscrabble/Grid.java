//
//package oscrabble;
//import org.apache.commons.collections4.Bag;
//import org.apache.commons.collections4.bag.TreeBag;
//import org.apache.log4j.Logger;
//import oscrabble.server.action.Action;
//import oscrabble.server.action.PlayTiles;
//import oscrabble.data.Tile;
//import oscrabble.server.Game;
//
//import java.text.ParseException;
//import java.util.*;
//
//public class Grid
//{
//	public static final Logger LOGGER = Logger.getLogger(Grid.class);
//
//	private final int size;
//	private final Square[][] squares;
////
////	/**
////	 * Information about the distribution of the letters
////	 */
////	private final IDictionary.ScrabbleRules scrabbleRules;
//
//	private Set<Square> allSquares;
//
//	public Grid(final int size)
//	{
//		this.size = size;
//
//		this.squares = new Square[size + 2][];
//		for (int x = 0; x < size + 2; x++)
//		{
//			this.squares[x] = new Square[size + 2];
//			for (int y = 0; y < size + 2; y++)
//			{
//				this.squares[x][y] = new Square(x, y);
//			}
//		}
//
////		this.scrabbleRules = scrabbleRules;
//	}
////
////	public Grid(final IDictionary.ScrabbleRules li)
////	{
////		this(li, SCRABBLE_SIZE);
////	}
//
//	/**
//	 *
//	 * @param x {@code 0} for border, {@code 1} for first line
//	 * @param y
//	 * @return
//	 */
//	public Square getSquare(final int x, final int y)
//	{
//		return this.squares[x][y];
//	}
//
//	public void set(final Square square, final Tile tile)
//	{
//		final Tile old = square.tile;
//		if (old != null)
//		{
//			throw new IllegalStateException("Square already assertContains " + old);
//		}
//		square.tile = tile;
//	}
//
//	public boolean isEmpty()
//	{
//		for (final Square square : getAllSquares())
//		{
//			if (!square.isEmpty())
//			{
//				return false;
//			}
//		}
//		return true;
//	}
//
//	/**
//	 * @return die Zelle in der Mitte des Spielfeldes.
//	 */
//	public Square getCenter()
//	{
//		assert this.size % 2 == 1;
//		final int center = (this.size + 1) / 2;
//		return getSquare(center, center);
//	}
//
//	public int getSize()
//	{
//		return this.size;
//	}
//
//	public Set<Square> getAllSquares()
//	{
//		if (this.allSquares == null)
//		{
//			this.allSquares = new LinkedHashSet<>();
//			for (int x = 1; x <= this.size; x++)
//			{
//				//noinspection ManualArrayToCollectionCopy
//				for (int y = 1; y <= this.size; y++)
//				{
//					this.allSquares.add(this.squares[x][y]);
//				}
//			}
//			this.allSquares = Collections.unmodifiableSet(this.allSquares);
//		}
//
//		return this.allSquares;
//	}
//
//	public Set<Square> getNeighbours(final Square square)
//	{
//		if (square.neighbours == null)
//		{
//			square.neighbours = new LinkedHashSet<>();
//			for (int dx : new int[]{-1, 0, 1})
//			{
//				for (final int dy : new int[]{-1, 0, 1})
//				{
//					if (dx == 0 ^ dy == 0)
//					{
//						final int nx = square.x + dx;
//						final int ny = square.y + dy;
//						final Square neighbour = getSquare(nx, ny);
//						if (!neighbour.isBorder())
//						{
//							square.neighbours.add(neighbour);
//						}
//					}
//				}
//			}
//			square.neighbours = Collections.unmodifiableSet(square.neighbours);
//		}
//		return square.neighbours;
//	}
//
//	/**
//	 *
//	 * @param square Square
//	 * @return the one or two words built on the square. The set is empty if the square is empty.
//	 */
//	public Set<String> getWords(final Square square)
//	{
//		Set<String> set = null;
//		for (final PlayTiles.Direction direction : PlayTiles.Direction.values())
//		{
//			String word;
//			if ((word = getWord(square, direction)) != null)
//			{
//				if (set == null)
//				{
//					set = new HashSet<>();
//				}
//				set.add(word);
//			}
//		}
//		return set == null ? Collections.emptySet() : set;
//	}
//
//	/**
//	 * Return the square described by its coordinate
//	 */
//	public Square getSquare(final String coordinate) throws ParseException
//	{
//		final PlayTiles playTiles = (PlayTiles) PlayTiles.parseMove(this, coordinate, true);
//		assert playTiles.word.isEmpty();
//		return playTiles.startSquare;
//	}
//
//
//	/**
//	 * Rollback a move.
//	 *
//	 * @param move information about the move to rollback
//	 */
//	public synchronized void remove(final MoveMetaInformation move)
//	{
//		for (final Square filledSquare : move.filledSquares)
//		{
//			filledSquare.tile = null;
//		}
//	}
//
//	public synchronized void put(final PlayTiles playTiles) throws ScrabbleException
//	{
//		int x = playTiles.startSquare.x;
//		int y = playTiles.startSquare.y;
//
//		LOGGER.info("Putting " + playTiles);
//
//		for (int i = 0; i < playTiles.word.length(); i++)
//		{
//			final char c = playTiles.word.charAt(i);
//			final Square sq = getSquare(x, y);
//			if (sq.isEmpty())
//			{
//				if (playTiles.isPlayedByBlank(i))
//				{
//					sq.tile = this.stoneGenerator.generateStone(null);
//					sq.tile.c = c;
//				}
//				else
//				{
//					sq.tile = this.stoneGenerator.generateStone(c);
//				}
//			}
//			else
//			{
//				sq.assertContainChar(c);
//			}
//
//			switch (playTiles.getDirection())
//			{
//				case HORIZONTAL:
//					x++;
//					break;
//				case VERTICAL:
//					y++;
//					break;
//			}
//		}
//	}
//
//
//	/**
//	 * Liefert den Bonus einer Zelle.
//	 * @param x {@code 0} for border
//	 */
//	private Bonus calculateBonus(final int x, final int y)
//	{
//
//		final int midColumn = this.size / 2 + 1;
//
//		if (x > midColumn)
//		{
//			return calculateBonus(this.size - x +1, y);
//		}
//		else if (y > midColumn)
//		{
//			return calculateBonus(x, this.size - y + 1);
//		}
//
//		if (x > y)
//		{
//			//noinspection SuspiciousNameCombination
//			return calculateBonus(y, x);
//		}
//
//		if (x == 0 || y == 0)
//		{
//			return Bonus.BORDER;
//		}
//
//		if (this.size != SCRABBLE_SIZE)
//		{
//			return Bonus.NONE;
//		}
//
//		assert (1 <= x && x <= y && y <= midColumn);
//
//
//		if (x == y)
//		{
//			switch (x)
//			{
//				case 1:
//					return Bonus.RED;
//				case 6:
//					return Bonus.DARK_BLUE;
//				case 7:
//					return Bonus.LIGHT_BLUE;
//				default:
//					return Bonus.ROSE;
//			}
//		}
//		else if (x == 1 && y == midColumn)
//		{
//			return Bonus.RED;
//		}
//		else if (x == 1 && y == 4)
//		{
//			return Bonus.LIGHT_BLUE;
//		}
//		else if (x == 2 && y == 6)
//		{
//			return Bonus.DARK_BLUE;
//		}
//		else if (x == 3 && y == 7)
//		{
//			return Bonus.LIGHT_BLUE;
//		}
//		else if (x == 4 && y == 8)
//		{
//			return Bonus.LIGHT_BLUE;
//		}
//		else
//		{
//			return Bonus.NONE;
//		}
//	}
//
//
//	public class Square
//	{
//		private final int x, y;
//		private final Bonus bonus;
//
//		public Tile tile;
//
//		private Set<Square> neighbours;
//
//		public Square(final int x, final int y)
//		{
//			this.x = x;
//			this.y = y;
//
//			this.bonus = calculateBonus(x, y);
//		}
//
//		public boolean isEmpty()
//		{
//			return this.tile == null;
//		}
//
//		public boolean isBorder()
//		{
//			return this.x == 0 || this.y == 0 || this.x == Grid.this.size + 1 || this.y == Grid.this.size + 1;
//		}
//
//		public int getX()
//		{
//			return this.x;
//		}
//
//		public int getY()
//		{
//			return this.y;
//		}
//
//		private Grid getGrid()
//		{
//			return Grid.this;
//		}
//
//		@Override
//		public boolean equals(final Object obj)
//		{
//			if (obj instanceof Square)
//			{
//				final Square other = (Square) obj;
//				return other.getGrid() == this.getGrid()
//						&& other.x == this.x
//						&& other.y == this.y;
//			}
//			else
//			{
//				return false;
//			}
//		}
//
//		@Override
//		public int hashCode()
//		{
//			return this.getGrid().hashCode() + this.x + this.y;
//		}
//
//		public boolean isLastOfLine(final PlayTiles.Direction direction)
//		{
//			return (direction == PlayTiles.Direction.HORIZONTAL ? this.x : this.y) == Grid.this.size;
//		}
//
//		@Override
//		public String toString()
//		{
//			final Character representation =
//					this.tile == null
//							? '_'
//							: this.tile.c == null ? '\u25A1' : this.tile.c;
//			return String.format("(%d,%d) %s", this.x, this.y, representation);
//		}
//
//
//		public Square getPrevious(final PlayTiles.Direction direction)
//		{
//			final int nx, ny;
//			if (direction == PlayTiles.Direction.HORIZONTAL)
//			{
//				nx = this.x - 1;
//				ny = this.y;
//			}
//			else
//			{
//				nx = this.x;
//				ny = this.y - 1;
//			}
//			return Grid.this.squares[nx][ny];
//		}
//
//		public boolean isFirstOfLine(final PlayTiles.Direction direction)
//		{
//			return direction == PlayTiles.Direction.HORIZONTAL ? this.x == 1 : this.y == 1;
//		}
//
//		public Square getFollowing(final PlayTiles.Direction direction)
//		{
//			final int nx, ny;
//			if (direction == PlayTiles.Direction.HORIZONTAL)
//			{
//				nx = this.x + 1;
//				ny = this.y;
//			}
//			else
//			{
//				nx = this.x;
//				ny = this.y + 1;
//			}
//			return Grid.this.squares[nx][ny];
//		}
//
//		public Bonus getBonus()
//		{
//			return this.bonus;
//		}
//
//		private void assertContainChar(final char c) throws ScrabbleException
//		{
//			if (this.isEmpty() || this.tile.c != c)
//			{
//				LOGGER.warn("Grid state:\n" + getGrid().asASCIIArt());
//				throw new ScrabbleException.ForbiddenPlayException("Square " + this.x + "," + this.y + " already occupied by " + this.tile.c);
//			}
//		}
//	}
//
//	public String asASCIIArt()
//	{
//		final StringBuilder sb = new StringBuilder();
//		final int gridSize = getSize();
//		for (int y = -1; y <= gridSize; y++)
//		{
//			for (int x = -1; x <= gridSize; x++)
//			{
//				if (x == -1 && y == -1)
//				{
//					sb.append("*");
//				}
//				else if (x == -1 && y == gridSize)
//				{
//					sb.append("*");
//				}
//				else if (x == gridSize && y == -1)
//				{
//					sb.append("*");
//				}
//				else if (x == gridSize && y == gridSize)
//				{
//					sb.append("*");
//				}
//				else if (x == -1 || x == gridSize)
//				{
//					sb.append(Integer.toHexString(y));
//				}
//				else if (y == -1 || y == gridSize)
//				{
//					sb.append(Integer.toHexString(x));
//				}
//				else
//				{
//					final Grid.Square square = this.getSquare(x, y);
//					sb.append(square.isEmpty() ? " " : square.tile.c);
//				}
//
//				if (x == gridSize)
//				{
//					sb.append("\n");
//				}
//			}
//		}
//		return sb.toString();
//	}
//
//
//	/**
//	 * @return Das Wort an diese Position und in der angegeben direction, {@code null} wenn kein Wort
//	 */
//	private String getWord(final Grid.Square position, final PlayTiles.Direction direction)
//	{
//		if (position.isEmpty())
//		{
//			return null;
//		}
//
//		Grid.Square square = position;
//		while (!(square.isBorder()) && !square.isEmpty())
//		{
//			square = square.getPrevious(direction);
//		}
//		final StringBuilder sb = new StringBuilder();
//		while (!(square = square.getFollowing(direction)).isEmpty() && !square.isBorder())
//		{
//			sb.append(square.tile.c);
//		}
//
//		if (sb.length() == 1)
//		{
//			return null;
//		}
//
//		return sb.toString();
//	}
//
//	/**
//	 * Definition der Bonus-Zellen.
//	 */
//	public enum Bonus
//	{
//		BORDER, NONE, RED(3, 1), ROSE(2, 1), DARK_BLUE(1, 3), LIGHT_BLUE(1, 2);
//
//		private final int wordFactor;
//		private final int charFactor;
//
//		Bonus()
//		{
//			this(1, 1);
//		}
//
//		Bonus(int wordFactor, int charFactor)
//		{
//			this.wordFactor = wordFactor;
//			this.charFactor = charFactor;
//		}
//	}
//}