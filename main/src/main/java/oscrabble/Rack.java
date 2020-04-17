package oscrabble;


import oscrabble.dictionary.Tile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Rack extends HashSet<Tile>
{
	public Rack()
	{
	}

	/**
	 * Entfernt Steine.
	 * @param characters die Buchstabe des Steins, {@code ' '} für ein Blank. Bei {@code null} werden alle Steine
	 *                   entfernt.
	 * @throws ScrabbleException falls nicht vorhanden.
	 */
	public List<Tile> removeStones(final char[] characters) throws ScrabbleException
	{
		final ArrayList<Tile> found = new ArrayList<>();
		boolean success = false;
		try
		{
			if (characters == null)
			{
				found.addAll(this);
				this.clear();
			}
			else
			{
				for (final Character c : characters)
				{
					Tile tile = searchLetter(c);
					if (tile == null)
					{
						throw new ScrabbleException.InvalidStateException("Not in the rack: " + c);
					}
					found.add(tile);
					this.remove(tile);
				}
			}
			success = true;
			return found;
		}
		finally
		{
			if (!success)
			{
				addAll(found);
			}
		}
	}

	public int countLetter(final Character letter)
	{
		int i = 0;
		for (final Tile tile : this)
		{
			if (!tile.isJoker() && tile.getChar() == letter)
			{
				i++;
			}
		}
		return i;
	}

	/**
	 * Findet ein Stein mit einer besonderen Buchstabe
	 * @param c die gesuchte Buchstabe, {@code ' '} für ein Blank
	 * @return das Stein oder {@code null}
	 */
	public Tile searchLetter(final Character c)
	{
		Tile found = null;
		for (final Tile tile : this)
		{
			if (c == ' ')
			{
				if (tile.isJoker())
				{
					found = tile;
					break;
				}
			}
			else if (!tile.isJoker() && tile.getChar() == c)
			{
				found = tile;
				break;
			}
		}
		return found;
	}

	public int countJoker()
	{
		int counter = 0;
		for (final Tile tile : this)
		{
			if (tile.isJoker())
			{
				counter++;
			}
		}
		return counter;
	}

	/**
	 *
	 * @return a new Rack with the same elements.
	 */
	public Rack copy()
	{
		final Rack copy = new Rack();
		copy.addAll(this);
		return copy;
	}

}
