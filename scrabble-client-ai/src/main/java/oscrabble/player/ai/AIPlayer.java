package oscrabble.player.ai;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.controller.MicroServiceScrabbleServer;
import oscrabble.data.*;
import oscrabble.data.objects.Grid;
import oscrabble.player.AbstractPlayer;

import java.time.Duration;
import java.util.*;

@SuppressWarnings("BusyWait")
public class AIPlayer extends AbstractPlayer {

	public static final Logger LOGGER = LoggerFactory.getLogger(AIPlayer.class);

	private final BruteForceMethod bruteForceMethod;

	@Setter
	Strategy strategy = new Strategy.BestScore(null, null);

	/**
	 * How long to wait before playing the found word (ms).
	 */
	@Setter
	Duration throttle = Duration.ofSeconds(0);

	@Setter
	Level level = Level.MIDDLE;

	private final UUID game;

	/**
	 * Daemon thread for this AI-player.
	 */
	final Thread daemonThread;
	private final MicroServiceScrabbleServer server;

	/**
	 * Construct an AI Player.
	 *
	 * @param game
	 * @param bruteForceMethod
	 */
	public AIPlayer(
			final BruteForceMethod bruteForceMethod,
			final UUID game,
			final UUID playerId,
			final MicroServiceScrabbleServer server
	) {
		super("AI");
		this.bruteForceMethod = bruteForceMethod;
		this.game = game;
		this.uuid = playerId;
		this.server = server;
		this.strategy = new Strategy.BestScore(server, game);
		this.throttle = Duration.ofSeconds(0);

		this.daemonThread = new Thread(() -> runDaemonThread());
		this.daemonThread.setDaemon(true);
		this.daemonThread.setName("AI Player Playing thread");
	}

	/**
	 * Start the daemon thread for the AI Player to response
	 */
	public void startDaemonThread() {
		if (this.server == null) {
			throw new AssertionError("No server");
		}

		this.daemonThread.start();
	}

	/**
	 * Run the thread
	 */
	private void runDaemonThread() {
		try {
			GameState state = null;
			do {
				final GameState newState = this.server.getState(this.game);
				if (!newState.equals(state)) {
					this.server.acknowledgeState(this.game, this.uuid, newState);
				}
				state = newState;
				if (state.state == GameState.State.STARTED && this.uuid.equals(state.getPlayerOnTurn())) {
					if (play(state)) {
						return;
					}
				}
				Thread.sleep(this.throttle.toMillis());
			} while (state.state != GameState.State.ENDED);

			LOGGER.info("Daemon thread of " + this.uuid + " ends.");
		} catch (final Throwable e) {
			LOGGER.error(e.toString(), e);
			// TODO: inform server and players
		}
	}

	/**
	 * Let the player play its turn: it select its word and send it to the server.
	 * @param state
	 * @return
	 * @throws Exception
	 */
	private boolean play(final GameState state) throws Exception {
		this.bruteForceMethod.setGrid(Grid.fromData(state.getGrid()));
		final ArrayList<Tile> rack = this.server.getRack(this.game, this.uuid).tiles;
		if (rack.isEmpty()) {
			System.out.println("Rack is empty");
			return true;
		}

		final ArrayList<Character> letters = new ArrayList<>();
		rack.forEach(t -> letters.add(t.c));

		String notation;
		try {
			final Set<String> legalMoves = this.bruteForceMethod.getLegalMoves(letters);
			final TreeMap<Integer, List<String>> valuedWords = this.strategy.sort(legalMoves);
			if (valuedWords == null) {
				notation = Action.PASS_TURN_NOTATION;
			} else {
				Integer selectedValue = (int) (valuedWords.lastKey() * this.level.factor);
				selectedValue = valuedWords.floorKey(selectedValue);
				notation = valuedWords.get(selectedValue).get(0);
			}
		} catch (Throwable e) {
			throw new Exception("Error finding a word with rack " + letters, e);
		}

		Thread.sleep(this.throttle.toMillis());
		final PlayActionResponse response = this.server.play(this.game, buildAction(notation));
		if (!response.success) {
			throw new AssertionError("Play of " + notation + "refused: " + response.message);
		}
		return false;
	}

	/**
	 * Difficulty level of an AI player
	 */
	public enum Level {
		VERY_SIMPLE(0.4f),
		SIMPLE(0.6f),
		MIDDLE(0.7f),
		HARD(0.8f),
		VERY_HARD(1);
		private final float factor;

		Level(final float factor) {
			this.factor = factor;
		}
	}
}
