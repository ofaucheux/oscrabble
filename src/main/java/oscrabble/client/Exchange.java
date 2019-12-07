package oscrabble.client;

import oscrabble.server.IPlay;

import java.util.ArrayList;
import java.util.List;

/**
 * Action für den Austausch von Buchsteben.
 */
public class Exchange implements IPlay
{
	final List<Character> chars;

	public Exchange(final ArrayList<Character> chars)
	{
		this.chars = chars;
	}

	public List<Character> getChars()
	{
		return this.chars;
	}
}
