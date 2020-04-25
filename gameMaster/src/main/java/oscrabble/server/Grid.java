package oscrabble.server;

import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.ScrabbleException;
import oscrabble.data.IDictionary;
import oscrabble.server.action.Action;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Grid
{
	public static final Logger LOGGER = LoggerFactory.getLogger(Grid.class);

	private final Square[][] squares;
	private final IDictionary.ScrabbleRules rules;

	Grid(final IDictionary.ScrabbleRules rules)
	{
		this.rules = rules;
		this.squares = new Square[rules.gridSize+1][];
		for (int x = 0; x < this.squares.length; x++)
		{
			this.squares[x] = new Square[rules.gridSize + 1];
			for (int y = 0; y < this.squares.length; y++)
			{
				this.squares[x][y] = new Square(x, y);
			}
		}
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
		final Triple<Action.Direction, Integer, Integer> triple = getCoordinate(coordinate);
		return this.get(triple.getMiddle(), triple.getRight()).c == null;
	}

	/**
	 * A square
	 */
	class Square
	{
		final int x;
		final int y;
		public int letterBonus;
		public int wordBonus;

		/**
		 * Action, which has filled this field.
		 */
		public Action action;

		Character c;

		Square(final int x, final int y)
		{
			this.x = x;
			this.y = y;
		}

		public boolean isEmpty()
		{
			return this.c == null;
		}

		public Square getNeightbourg(final Action.Direction direction, int value)
		{
			return Grid.this.get(
					this.x + (direction == Action.Direction.HORIZONTAL ? value : 0),
					this.y + (direction == Action.Direction.VERTICAL ? value : 0)
			);
		}

		public boolean isBorder()
		{
			return this.x == 0 || this.x == Grid.this.rules.gridSize + 1
					|| this.y == 0 || this.y == Grid.this.rules.gridSize + 1;
		}
	}

	public static class MoveMetaInformation
	{
		public static final Comparator<MoveMetaInformation> WORD_LENGTH_COMPARATOR = (meta1, meta2) -> meta1.requiredLetter.size() - meta2.requiredLetter.size();
		public static final Comparator<Grid.MoveMetaInformation> SCORE_COMPARATOR = (meta1, meta2) -> meta1.score - meta2.score;

		public List<String> crosswords = new ArrayList<>();
		final Action.PlayTiles playTiles;
		public boolean isScrabble;

		/**
		 * Square the move will fill and which are not filled for the move
		 */
		private final ArrayList<Square> filledSquares = new ArrayList<>();

		int score;
		private int requiredBlanks;
		final List<Character> requiredLetter = new ArrayList<>();

		MoveMetaInformation(final Action.PlayTiles playTiles)
		{
			this.playTiles = playTiles;
		}

		public int getScore()
		{
			return this.score;
		}

		public Iterable<Square> getFilledSquares()
		{
			return this.filledSquares;
		}
	}

	public MoveMetaInformation getMetaInformation(final Action.PlayTiles playTiles) throws ScrabbleException
	{
		int x = playTiles.x;
		int y = playTiles.y;

		LOGGER.trace("Scoring " + playTiles);

		final MoveMetaInformation mmi = new MoveMetaInformation(playTiles);
		int wordFactor = 1;
		int crosswordScores = 0;
		for (int i = 0; i < playTiles.word.length(); i++)
		{
			final char c = playTiles.word.charAt(i);
			final boolean isBlank = Character.isLowerCase(c);

			final Square sq = this.squares[x][y];
			if (sq.isEmpty())
			{
				mmi.filledSquares.add(sq);
				mmi.requiredLetter.add(isBlank ? ' ' : c);

				final int stoneScore = isBlank
						? 0
						: getPoints(c) * sq.letterBonus;

				mmi.score += stoneScore;
				wordFactor *= sq.wordBonus;

				// Berechnet die Querwörter und ihre Scores
				final StringBuilder crossword = new StringBuilder();
				Square cursor;
				cursor = sq;
				final Action.PlayTiles.Direction crossDirection = playTiles.direction.other();
				int crosswordScore = 0;
				while (!(cursor = cursor.getNeightbourg(crossDirection, -1)).isBorder() && !cursor.isEmpty())
				{
					crossword.insert(0, cursor.c);
					if (Character.isUpperCase(cursor.c))
					{
						crosswordScore += getPoints(cursor.c);
					}
				}

				crossword.append(c);
				crosswordScore += getPoints(c) * sq.letterBonus;

				cursor = sq;
				while (!(cursor = cursor.getNeightbourg(crossDirection, 1)).isBorder() && !cursor.isEmpty())
				{
					crossword.append(cursor.c);
					if (Character.isUpperCase(cursor.c))
					{
						crosswordScore += getPoints(cursor.c);
					}
				}

				if (crossword.length() > 1)
				{
					crosswordScores += crosswordScore * sq.wordBonus;
					mmi.crosswords.add(crossword.toString());
				}
			}
			else
			{
				if (Character.toUpperCase(c) != Character.toUpperCase(sq.c))
				{
					throw new ScrabbleException.ForbiddenPlayException("Square " + sq.x + "," + sq.y + " already occupied by " + sq.c);
				}
				mmi.score += getPoints(sq.c);
			}

			switch (playTiles.direction)
			{
				case HORIZONTAL:
					x++;
					break;
				case VERTICAL:
					y++;
					break;
			}
		}
		mmi.score *= wordFactor;
		mmi.score += crosswordScores;

		// scrabble-bonus
		if (mmi.requiredLetter.size() + mmi.requiredBlanks == Game.RACK_SIZE)
		{
			mmi.isScrabble = true;
			mmi.score += 50;
		}

		return mmi;
	}

	private int getPoints(final Character c)
	{
		return this.rules.letters.get(c).points;
	}

	private static final Pattern VERTICAL_COORDINATE_PATTERN = Pattern.compile("(\\d+)(\\w)");
	private static final Pattern HORIZONTAL_COORDINATE_PATTERN = Pattern.compile("(\\w)(\\d+)");

	public static Triple<Action.Direction, Integer, Integer> getCoordinate(final String notation) throws ScrabbleException.ForbiddenPlayException
	{
		final Action.Direction direction;
		final int groupX, groupY;
		Matcher m;
		if ((m = HORIZONTAL_COORDINATE_PATTERN.matcher(notation)).matches())
		{
			direction = Action.Direction.HORIZONTAL;
			groupX = 2;
			groupY = 1;
		}
		else if ((m = VERTICAL_COORDINATE_PATTERN.matcher(notation)).matches())
		{
			direction = Action.Direction.VERTICAL;
			groupX = 1;
			groupY = 2;
		}
		else
		{
			throw new ScrabbleException.ForbiddenPlayException("Cannot parse coordinate: " + notation);
		}

		return Triple.of(
				direction,
				Integer.parseInt(m.group(groupX)),
				m.group(groupY).charAt(0) - 'A' + 1
		);
	}
}

