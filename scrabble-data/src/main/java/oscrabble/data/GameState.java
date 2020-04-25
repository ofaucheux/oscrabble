package oscrabble.data;

import java.util.List;
import java.util.Set;

public class GameState
{
	/**
	 * State of the game
	 */
	public enum State {BEFORE_START, STARTED, ENDED,}

	State state;
	Set<Player> players;
	List<Action> actions;
	Grid grid;
	Bag bag;
}
