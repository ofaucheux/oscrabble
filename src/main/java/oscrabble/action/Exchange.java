package oscrabble.action;

/**
 * Action für den Austausch von Buchstaben.
 */
public class Exchange implements Action
{
	final char[] chars;

	public Exchange(final String chars)
	{
		this.chars = chars.toCharArray();
	}

	public char[] getChars()
	{
		return this.chars;
	}

	@Override
	public String getNotation()
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("- ");
		for (final char c : this.chars)
		{
			sb.append(c);
		}
		return sb.toString();
	}
}
