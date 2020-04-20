package oscrabble.action;

import oscrabble.Grid;
import oscrabble.server.Tile;

import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayTiles implements Action
{
	public final Grid.Square startSquare;
	final Direction direction;

	/** The word created by this move, incl. already set tiles and where blanks are represented by their value letters. */
	public String word;
	private final String originalWord;

	/**
	 * Die Blanks (mindesten neugespielt) werden durch klein-buchstaben dargestellt.
	 */
	public PlayTiles(final Grid.Square startSquare,
					 final Direction direction,
					 final String word)
	{
		this.startSquare = startSquare;
		this.direction = direction;
		this.originalWord = word;
		this.word = word.toUpperCase();
	}

	public static final Pattern PASS_TURN = Pattern.compile("-");
	public static final Pattern EXCHANGE = Pattern.compile("-\\s+\\S+");
	public static final Pattern HORIZONTAL_COORDINATE_PATTERN = Pattern.compile("((\\d+)(\\w))(\\s+(\\S*))?");
	public static final Pattern VERTICAL_COORDINATE_PATTERN = Pattern.compile("((\\w)(\\d+))(\\s+(\\S*))?");

	/**
	 *
	 * @param grid grid of game
	 * @param sli dictionary of game
	 * @return all stones of the move, even if they already are on the board.
	 */
	public LinkedHashMap<Grid.Square, Tile> getStones(final Grid grid, final Tile.Generator sli)
	{
		final LinkedHashMap<Grid.Square, Tile> stones = new LinkedHashMap<>();
		int y = this.startSquare.getY();
		int x = this.startSquare.getX();
		for (int i = 0; i < this.word.length(); i++)
		{
			char c = this.originalWord.charAt(i);
			stones.put(grid.getSquare(x, y), sli.generateStone(Character.isLowerCase(c) ? null : c));
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
	 * @param grid            die
	 * @param coordinate      die Beschreibung, z.B. {@code B4 WAGEN} für Horizontal, B, 4 Wort WAGEN.
	 * @return der Spielzug
	 * @throws ParseException wenn aus der Beschreibung keinen Spielzug zu finden ist.
	 */
	public static Action parseMove(final Grid grid, final String coordinate) throws ParseException
	{
		return parseMove(grid, coordinate, false);
	}


	/**
	 * Parse die Beschreibung eines Spielzuges.
	 *
	 * @param grid          die
	 * @param coordinate    die Beschreibung, z.B. {@code B4 WAGEN} für Horizontal, B, 4 Wort WAGEN.
	 * @param acceptEmptyWord -
	 * @return der Spielzug
	 * @throws ParseException wenn aus der Beschreibung keinen Spielzug zu finden ist.
	 */
	public static Action parseMove(final Grid grid, final String coordinate, final boolean acceptEmptyWord) throws ParseException
	{
		final PlayTiles.Direction direction;
		final int groupX;
		final int groupY;
		Matcher matcher;
		if (PASS_TURN.matcher(coordinate).matches())
		{
			return SkipTurn.SINGLETON;
		}
		else if ((matcher = EXCHANGE.matcher(coordinate)).matches())
		{
			return new Exchange(matcher.group(1));
		}
		if ((matcher = HORIZONTAL_COORDINATE_PATTERN.matcher(coordinate)).matches())
		{
			direction = PlayTiles.Direction.HORIZONTAL;
			groupX = 3;
			groupY = 2;
		}
		else if ((matcher = VERTICAL_COORDINATE_PATTERN.matcher(coordinate)).matches())
		{
			direction = PlayTiles.Direction.VERTICAL;
			groupX = 2;
			groupY = 3;
		}
		else
		{
			throw new ParseException(coordinate, 0);
		}

		final int x = (Character.toUpperCase(matcher.group(groupX).charAt(0)) - 'A' + 1);
		final int y = Integer.parseInt(matcher.group(groupY));
		final String word = matcher.group(5);

		if (x == 0 || y == 0 || x > 15 || y > 15)
		{
			throw new ParseException("Invalid coordinates: " + matcher.group(1), 0);
		}

		if (!acceptEmptyWord && (word == null || word.isEmpty()))
		{
			throw new ParseException("Missing word", 0);
		}
		return new PlayTiles(grid.getSquare(x, y), direction, word == null ? "" : word);
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
		if (!(other instanceof PlayTiles))
		{
			return false;
		}
		return this.startSquare.equals(((PlayTiles) other).startSquare)
				&& this.direction == ((PlayTiles) other).direction
				&& this.originalWord.equals(((PlayTiles) other).originalWord);
	}

	@Override
	public String toString()
	{
		return getNotation();
	}

	@Override
	public String getNotation()
	{
		final String column = Character.toString((char) ('A' - 1 + this.startSquare.getX()));
		final String line = Integer.toString(this.startSquare.getY());
		final StringBuilder sb = new StringBuilder();
		sb.append((this.direction == Direction.HORIZONTAL) ? line + column : column + line);
		if (this.originalWord != null && !this.originalWord.isEmpty())
		{
			final char[] letters = this.originalWord.toCharArray();
			for (int i = 0; i < letters.length; i++)
			{
				if (isPlayedByBlank(i))
				{
					letters[i] = Character.toLowerCase(letters[i]);
				}
			}
			sb.append(" ").append(String.valueOf(letters));
		}
		return sb.toString();
	}

	/**
	 * @return eine Kopie des Spielzuges in der anderen Richtung: selbes Wort, selbe Startzelle.
	 */
	public PlayTiles getInvertedDirectionCopy()
	{
		return new PlayTiles(this.startSquare, this.direction.other(), this.word);
	}

	/**
	 * @param newStartSquare das neue Startfeld
	 * @return eine Kopie des Spielzuges mit einer anderen Startfeld.
	 */
	public PlayTiles getTranslatedCopy(Grid.Square newStartSquare)
	{
		return new PlayTiles(newStartSquare, this.direction, this.word);
	}

}
