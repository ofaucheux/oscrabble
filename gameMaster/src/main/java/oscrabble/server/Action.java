package oscrabble.server;

public class Action extends oscrabble.data.Action
{
	/**
	 * Unique existing instance of this action.
	 */
	public static final Action PASS_TURN = new Action();
	static
	{
		PASS_TURN.setNotation("-");
	}

	public void setNotation(final String notation)
	{
		this.notation = notation;
	}
}
