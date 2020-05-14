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

//		private ComparatorSelector selector;

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
		configuration = new BruteForceMethod.Configuration();
		configuration.strategy = new Strategy.BestScore();
		configuration.throttle = 2;

		daemonThread = new Thread(() -> runDaemonThread());
		daemonThread.setDaemon(true);
		daemonThread.setName("AI Player Playing thread");
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
				state = server.getState(game);
				if (this.uuid.equals(state.getPlayerOnTurn()))
				{
					this.bruteForceMethod.grid = Grid.fromData(state.getGrid());
					final ArrayList<Tile>  rack = server.getRack(game, this.uuid).tiles;
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
					server.play(game, action);
					//				System.out.println(server.getState(game).state);
				}
			} while (state.state != GameState.State.ENDED);

			LOGGER.info("Daemon thread of " + uuid + " ends.");
		}
		catch (final Throwable e)
		{
			LOGGER.error(e.toString(), e);
			// TODO: inform server and players
		}
	}

	public void onPlayRequired(final AIPlayer player, final Collection<Character> rack) throws ScrabbleException.NotInTurn
	{
		if (player != AIPlayer.this)
		{
			return;
		}

		try
		{
			Set<String> possibleMoves = new HashSet<>(bruteForceMethod.getLegalMoves(rack));

			if (possibleMoves.isEmpty())
			{
				// todo				this.game.sendMessage(this, "No possible moves anymore");
				this.server.play(game, this.buildAction("-"));
			}
			else
			{
//					final Configuration configuration = this.getConfiguration();
				if (configuration.throttle > 0)
				{
					LOGGER.trace("Wait " + configuration.throttle + " seconds...");
					Thread.sleep(configuration.throttle * 1000);
				}

				final LinkedList<String> sortedMoves = new LinkedList<>(possibleMoves);
				this.configuration.strategy.sort(sortedMoves);
				if (this.uuid.equals(this.server.getPlayerOnTurn(game)))  // check the player still is on turn and no rollback toke place.
				{
					LOGGER.info("Play " + sortedMoves.getFirst());
					this.server.play(game, buildAction(sortedMoves.getFirst()));
				}
			}
		}
		catch (ScrabbleException e)
		{
			throw new Error(e);
		}
		catch (InterruptedException e)
		{
			LOGGER.info("Has been interrupted");
			// TODO inform server
			Thread.currentThread().interrupt();
		}
	}

	public void onDispatchMessage(String msg)
	{
		LOGGER.debug("Received message: " + msg);
	}
//
//		public void beforeGameStart()
//		{
//			updateConfiguration();
//		}

//		public void editParameters()
//		{
//
//			final JPanel panel = new JPanel();
//			panel.setBorder(new TitledBorder("Parameters"));
//			panel.add(new ConfigurationPanel(this.configuration));
//
//			final JScrollPane sp = new JScrollPane(panel);
//			sp.setBorder(null);
//			JOptionPane.showOptionDialog(
//					null,
//					sp,
//					"Options for " + getName(),
//					JOptionPane.DEFAULT_OPTION,
//					JOptionPane.PLAIN_MESSAGE,
//					null,
//					null,
//					null
//			);
//
//			updateConfiguration();
//		}

//		protected void updateConfiguration()
//		{
//			final Supplier<Grid> gridSupplier = () -> Player.this.game.getGrid();
//			final Configuration configuration = getConfiguration();
//			this.selector = new ComparatorSelector(gridSupplier, configuration.strategy.valuator);
//			this.selector.setMean(configuration.force / 100f);
//		}
//
//		/**
//		 * Load or update the configuration from a properties set.
//		 * @param properties properties to configure the player with.
//		 */
//		public void loadConfiguration(final Properties properties)
//		{
//			this.configuration.loadProperties(properties);
//		}

//		@Override
//		public Configuration getConfiguration()
//		{
//			return (Configuration) this.configuration;
//		}


}
