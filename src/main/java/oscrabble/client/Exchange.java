package oscrabble.client;

import oscrabble.Move;
import oscrabble.server.IAction;

import java.util.ArrayList;
import java.util.List;

/**
 * Action für den Austausch von Buchsteben.
 */
public class Exchange implements IAction
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
