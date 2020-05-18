package oscrabble.data;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class GameState
{
	/**
	 * ID of the game
	 */
	public UUID gameId;

	/**
	 * State of the game
	 */
	public State state;

	public List<Player> players;

	/**
	 * Id of the player on turn, null if no such
	 */
	public UUID playerOnTurn;

	public List<Action> playedActions;

	public Grid grid;
	public Bag bag;

	/**
	 * Actual turn id. The turn is 0 until the game has started.
	 */
	public int turnId;

	/**
	 * State of a game
	 */
	public enum State
	{BEFORE_START, STARTED, ENDED,}

}
