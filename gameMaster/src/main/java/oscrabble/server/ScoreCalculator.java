package oscrabble.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.ScrabbleException;
import oscrabble.data.ScrabbleRules;
import oscrabble.data.objects.Grid;
import oscrabble.controller.Action;

import java.util.ArrayList;
import java.util.List;

public class ScoreCalculator
{
	public static final Logger LOGGER = LoggerFactory.getLogger(ScoreCalculator.class);

	private final ScrabbleRules rules;

	public ScoreCalculator(final ScrabbleRules rules)
	{
		this.rules = rules;
	}

	public MoveMetaInformation getMetaInformation(
			final Grid grid,
			final Action.PlayTiles action
	) throws ScrabbleException
	{
		LOGGER.trace("Scoring " + action);

		final MoveMetaInformation mmi = new MoveMetaInformation(action);

		int wordFactor = 1;
		int crosswordScores = 0;
		Grid.Square sq = grid.get(action.startSquare);
		for (int i = 0; i < action.word.length(); i++)
		{
			final char c = action.word.charAt(i);
			final boolean isBlank = Character.isLowerCase(c);

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
				Grid.Square cursor;
				cursor = sq;
				final Grid.Direction crossDirection = action.startSquare.direction.other();
				int crosswordScore = 0;
				while (!(cursor = cursor.getNeighbour(crossDirection, -1)).isBorder() && !cursor.isEmpty())
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
				while (!(cursor = cursor.getNeighbour(crossDirection, 1)).isBorder() && !cursor.isEmpty())
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
				if (Character.isUpperCase(sq.c))
				{
					mmi.score += getPoints(c) * sq.letterBonus;
				}
			}

			sq = sq.getNeighbour(action.startSquare.direction, 1);
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


	public static class MoveMetaInformation
	{
		public final Action.PlayTiles action;

		public final List<String> crosswords = new ArrayList<>();


		//		final Action.PlayTiles playTiles;
		public boolean isScrabble;

		/**
		 * Square the move will fill and which are not filled for the move
		 */
		private final ArrayList<Grid.Square> filledSquares = new ArrayList<>();

		int score;
		private int requiredBlanks;
		// todo: not public
		public final List<Character> requiredLetter = new ArrayList<>();

		MoveMetaInformation(final Action.PlayTiles action)
		{
			this.action = action;
		}

		public int getScore()
		{
			return this.score;
		}

		public Iterable<Grid.Square> getFilledSquares()
		{
			return this.filledSquares;
		}
	}

	private int getPoints(final Character c)
	{
		return Character.isLowerCase(c)
				? 0
				: this.rules.letters.get(c).points;
	}

}
