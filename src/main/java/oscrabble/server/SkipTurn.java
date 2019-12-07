package oscrabble.server;

/**
 * Action of skipping its turn.
 */
public class SkipTurn implements IPlay
{
	/**
	 * Unique existing instance of this action.
	 */
	public static final SkipTurn SINGLETON = new SkipTurn();

	private SkipTurn(){};
}
