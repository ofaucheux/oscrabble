package oscrabble.client;

import oscrabble.server.Action;

import java.util.ArrayList;
import java.util.List;

/**
 * Action für den Austausch von Buchsteben.
 */
public class Exchange implements Action
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
