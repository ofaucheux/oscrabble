package oscrabble.data;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
@Builder
public class GameState
{
	/**
	 * State of the game
	 */
	public enum State
	{BEFORE_START, STARTED, ENDED,}

	public State state;
	public List<Player> players;
	/**
	 * Id of the player on turn, null if no such
	 */
	public String playerOnTurn;
	public List<Action> playedActions;
	public Grid grid;
	public Bag bag;
}
