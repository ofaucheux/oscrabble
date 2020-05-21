package oscrabble.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.ScrabbleException;
import oscrabble.data.ScrabbleRules;
import oscrabble.data.objects.Grid;
import oscrabble.controller.Action;
import oscrabble.data.objects.Square;

import java.util.ArrayList;
import java.util.List;

public class ScoreCalculator
{
	public static final Logger LOGGER = LoggerFactory.getLogger(ScoreCalculator.class);

	static public MoveMetaInformation getMetaInformation(
			final Grid grid,
			final ScrabbleRules rules,
			final Action.PlayTiles action
	) throws ScrabbleException
	{
		LOGGER.trace("Scoring " + action);

		final MoveMetaInformation mmi = new MoveMetaInformation(action);

		int wordFactor = 1;
		int crosswordScores = 0;
		Square sq = grid.get(action.startSquare);
		for (int i = 0; i < action.word.length(); i++)
		{
			final char c = action.word.charAt(i);
			final boolean isBlank = Character.isLowerCase(c);

			if (sq.isEmpty())
			{
				mmi.requiredLetter.add(isBlank ? ' ' : c);

				mmi.score += getPoints(c, rules) * sq.letterBonus;
				wordFactor *= sq.wordBonus;

				// Berechnet die Querwörter und ihre Scores
				final StringBuilder crossword = new StringBuilder();
				Square cursor;
				cursor = sq;
				final Grid.Direction crossDirection = action.startSquare.direction.other();
				int crosswordScore = 0;
				while (!(cursor = grid.getNeighbour(cursor, crossDirection, -1)).isBorder() && !cursor.isEmpty())
				{
					crossword.insert(0, cursor.tile.c);
					crosswordScore += cursor.tile.points;
				}

				crossword.append(c);

				cursor = sq;
				while (!(cursor = grid.getNeighbour(cursor, crossDirection, 1)).isBorder() && !cursor.isEmpty())
				{
					crossword.append(cursor.tile.c);
					crosswordScore += cursor.tile.points;
				}

				if (crossword.length() > 1)
				{
					crosswordScore += getPoints(c, rules) * sq.letterBonus;
					crosswordScores += crosswordScore * sq.wordBonus;
					mmi.crosswords.add(crossword.toString());
				}
			}
			else
			{
				if (Character.toUpperCase(c) != Character.toUpperCase(sq.tile.c))
				{
					throw new ScrabbleException.ForbiddenPlayException("Square " + sq + " already occupied by " + sq.tile);
				}
				mmi.score += sq.tile.points * sq.letterBonus;
			}

			sq = grid.getNeighbour(sq, action.startSquare.direction, 1);
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

	private static int getPoints(final Character c, final ScrabbleRules rules)
	{
		return Character.isLowerCase(c)
				? 0
				: rules.letters.get(c).points;
	}

	public static class MoveMetaInformation
	{
		public final Action.PlayTiles action;

		public final List<String> crosswords = new ArrayList<>();


		//		final Action.PlayTiles playTiles;
		public boolean isScrabble;

		int score;

		int requiredBlanks;

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

	}

}
