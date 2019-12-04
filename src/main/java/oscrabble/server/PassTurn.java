package oscrabble.server;

/**
 * Action of passing its turn.
 */
public class PassTurn implements IAction
{
	/**
	 * Unique existing instance of this action.
	 */
	public static final PassTurn SINGLETON = new PassTurn();

	private PassTurn(){};
}
