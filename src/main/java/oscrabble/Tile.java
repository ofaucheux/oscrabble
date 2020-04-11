package oscrabble;

import oscrabble.action.Action;

/**
 * Definition eines Steins im Spiel: Letter (oder Blank), Wert usw.
 */
public class Tile
{
	public final static Generator SIMPLE_GENERATOR = new SimpleGenerator();

	private final boolean isJoker;
	private Character c;
	private final int points;

	/**
	 * Der Spielzug, bei welchem der Stein gespielt wurde.
	 */
	private Action settingAction;

	public Tile(final Character c, final int point)
	{
		this.c = c;
		this.points = point;
		this.isJoker = c == null;
	}

	@Override
	public String toString()
	{
		return this.isJoker ? " - joker - " : Character.toString(this.c);
	}

	/**
	 * @return the letter of this stone
	 * @throws AssertionError if the stone is a still not played joker.
	 */
	public char getChar()
	{
		if (this.isJoker && this.c == null)
		{
			throw new AssertionError("White stone not set");
		}
		return this.c;
	}

	/**
	 * @return the letter of this stone or a space for a still not played joker.
	 */
	public char getCharOrSpace()
	{
		if (this.isJoker)
		{
			return ' ';
		}
		else
		{
			return getChar();
		}
	}

	public int getPoints()
	{
		return this.points;
	}

	public boolean isJoker()
	{
		return this.isJoker;
	}

	public boolean hasCharacterSet()
	{
		return this.c != null;
	}

	public void setCharacter(final char c)
	{
		if (!this.isJoker)
		{
			throw new AssertionError("Cannot exchange character of no blank tile");
		}

		if (this.c != null)
		{
			throw new AssertionError("Blank title character already has been set.");
		}

		this.c = c;
	}

	public void setSettingAction(final Action settingAction)
	{
		this.settingAction = settingAction;
	}

	public Action getSettingAction()
	{
		return this.settingAction;
	}

	public interface Generator
	{
		/**
		 * @param c {@code null} für ein Blank
		 * @return ein neues Stein.
		 */
		Tile generateStone(final Character c);
	}

	private static class SimpleGenerator implements Generator
	{
		@Override
		public Tile generateStone(final Character c)
		{
			final Tile tile = new Tile(c, 1);
			return tile;
		}
	}
}
