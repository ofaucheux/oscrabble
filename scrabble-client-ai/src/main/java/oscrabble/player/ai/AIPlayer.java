package oscrabble.player.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.controller.MicroServiceScrabbleServer;
import oscrabble.data.*;
import oscrabble.data.objects.Grid;
import oscrabble.player.AbstractPlayer;

import java.util.*;

@SuppressWarnings("BusyWait")
public class AIPlayer extends AbstractPlayer {
	public static final Logger LOGGER = LoggerFactory.getLogger(AIPlayer.class);
	private final BruteForceMethod bruteForceMethod;
	private final BruteForceMethod.Configuration configuration;

	private final UUID game;

	/**
	 * Daemon thread for this AI-player.
	 */
	final Thread daemonThread;
	private final MicroServiceScrabbleServer server;

	/**
	 * How long to wait before playing the found word (ms).
	 */
	private long throttle = 0;

	/**
	 * Construct an AI Player.
	 *
	 * @param game
	 * @param bruteForceMethod
	 */
	public AIPlayer(final BruteForceMethod bruteForceMethod, final UUID game, final UUID playerId, final MicroServiceScrabbleServer server) {
		super("AI");
		this.bruteForceMethod = bruteForceMethod;
		this.game = game;
		this.uuid = playerId;
		this.server = server;
//			super(new Configuration(), name);
		this.configuration = new BruteForceMethod.Configuration();
		this.configuration.strategy = new Strategy.BestScore(server, game);
		this.configuration.throttle = 2;

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
				Thread.sleep(this.throttle);
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

		List<String> moves;
		try {
			moves = new ArrayList<>(this.bruteForceMethod.getLegalMoves(
					letters,
					this.configuration.strategy));
		} catch (Throwable e) {
			throw new Exception("Error finding a word with rack " + letters, e);
		}

		final String notation = moves.isEmpty()
				? Action.PASS_TURN_NOTATION
				: moves.get(0);

		Thread.sleep(this.throttle);
		final PlayActionResponse response = this.server.play(this.game, buildAction(notation));
		if (!response.success) {
			throw new AssertionError("Play of " + notation + "refused: " + response.message);
		}
		return false;
	}

	public void setThrottle(final long throttle) {
		this.throttle = throttle;
	}

//		@Override
//		public Configuration getConfiguration()
//		{
//			return (Configuration) this.configuration;
//		}


}
