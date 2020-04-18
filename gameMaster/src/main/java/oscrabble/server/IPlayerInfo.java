package oscrabble.server;

public interface IPlayerInfo
{
	String getName();
	int getScore();

	/**
	 * @return ob der Player eine Möglichkeit anbietet, seine Parameters zu ändern
	 */
	boolean hasEditableParameters();
}
