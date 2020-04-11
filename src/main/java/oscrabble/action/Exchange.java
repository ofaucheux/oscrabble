package oscrabble.action;

import java.util.ArrayList;
import java.util.List;

/**
 * Action für den Austausch von Buchstaben.
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

	@Override
	public String getNotation()
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("- ");
		this.chars.forEach(c -> sb.append(c));
		return sb.toString();
	}
}
