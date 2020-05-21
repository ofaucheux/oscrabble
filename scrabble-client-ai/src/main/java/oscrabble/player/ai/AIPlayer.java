package oscrabble.player.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.ScrabbleException;
import oscrabble.controller.MicroServiceScrabbleServer;
import oscrabble.data.*;
import oscrabble.data.objects.Grid;
import oscrabble.player.AbstractPlayer;

import java.util.*;

public class AIPlayer extends AbstractPlayer
{
	public static final Logger LOGGER = LoggerFactory.getLogger(AIPlayer.class);
	private final BruteForceMethod bruteForceMethod;
	private final BruteForceMethod.Configuration configuration;

	private final UUID game;
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
	public AIPlayer(final BruteForceMethod bruteForceMethod, final UUID game, final UUID playerId, final MicroServiceScrabbleServer server)
	{
		super("AI");
		this.bruteForceMethod = bruteForceMethod;
		this.game = game;
		this.uuid = playerId;
		this.server = server;
//			super(new Configuration(), name);
		this.configuration = new BruteForceMethod.Configuration();
		this.configuration.strategy = new Strategy.BestScore();
		this.configuration.throttle = 2;

		this.daemonThread = new Thread(() -> runDaemonThread());
		this.daemonThread.setDaemon(true);
		this.daemonThread.setName("AI Player Playing thread");
	}

	/**
	 * Start the daemon thread for the AI Player to response
	 */
	public void startDaemonThread()
	{
		if (this.server == null)
			throw new AssertionError("No server");

		this.daemonThread.start();
	}

	/**
	 * Run the thread
	 */
	private void runDaemonThread()
	{
		try
		{
			GameState state;
			do
			{
				Thread.sleep(this.throttle);
				state = this.server.getState(this.game);
				if (state.state == GameState.State.STARTED && this.uuid.equals(state.getPlayerOnTurn()))
				{
					this.bruteForceMethod.grid = Grid.fromData(state.getGrid());
					final ArrayList<Tile>  rack = this.server.getRack(this.game, this.uuid).tiles;
					final Player player0 = state.getPlayers().get(0);
					if (rack.isEmpty())
					{
						System.out.println("Rack is empty");
						return;
					}

					final ArrayList<Character> letters = new ArrayList<>();
					rack.forEach(t -> letters.add(t.c));

					final ArrayList<String> moves = new ArrayList<>(this.bruteForceMethod.getLegalMoves(letters));
					moves.sort((o1, o2) -> o2.length() - o1.length());
					if (moves.isEmpty())
					{
						System.out.println("No move anymore, Rack: " + rack);
						return;
					}
					final Action action = buildAction(moves.get(0));
					System.out.println("Plays: " + action.notation);
					final PlayActionResponse response = this.server.play(this.game, action);
					if (!response.success)
					{
						throw new AssertionError(response.message);
					}
				}
			} while (state.state != GameState.State.ENDED);

			LOGGER.info("Daemon thread of " + this.uuid + " ends.");
		}
		catch (final Throwable e)
		{
			LOGGER.error(e.toString(), e);
			// TODO: inform server and players
		}
	}

	public void setThrottle(final long throttle)
	{
		this.throttle = throttle;
	}

//		@Override
//		public Configuration getConfiguration()
//		{
//			return (Configuration) this.configuration;
//		}


}
