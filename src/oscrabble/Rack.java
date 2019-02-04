package oscrabble;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Rack extends HashSet<Stone>
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
	public List<Stone> removeStones(final List<Character> characters) throws ScrabbleException
	{
		final ArrayList<Stone> found = new ArrayList<>();
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
					Stone stone = searchLetter(c);
					if (stone == null)
					{
						throw new ScrabbleException(ScrabbleException.ERROR_CODE.FORBIDDEN,
								"Not in the rack: " + characters);
					}
					found.add(stone);
					this.remove(stone);
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
		for (final Stone stone : this)
		{
			if (!stone.isJoker() && stone.getChar() == letter)
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
	public Stone searchLetter(final Character c)
	{
		Stone found = null;
		for (final Stone stone : this)
		{
			if (c == ' ')
			{
				if (stone.isJoker())
				{
					found = stone;
					break;
				}
			}
			else if (!stone.isJoker() && stone.getChar() == c)
			{
				found = stone;
				break;
			}
		}
		return found;
	}

	public int countJoker()
	{
		int counter = 0;
		for (final Stone stone : this)
		{
			if (stone.isJoker())
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
		for (final Stone stone : this)
		{
			copy.add(stone);
		}
		return copy;
	}

}
