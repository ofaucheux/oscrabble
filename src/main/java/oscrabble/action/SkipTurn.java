package oscrabble.action;

/**
 * Action of skipping its turn.
 */
public class SkipTurn implements Action
{
	/**
	 * Unique existing instance of this action.
	 */
	public static final SkipTurn SINGLETON = new SkipTurn();

	private SkipTurn(){};
}
