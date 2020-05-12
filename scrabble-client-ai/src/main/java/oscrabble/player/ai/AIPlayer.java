package oscrabble.player.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.ScrabbleException;
import oscrabble.controller.MicroServiceScrabbleServer;
import oscrabble.data.Action;
import oscrabble.data.Bag;
import oscrabble.data.Player;
import oscrabble.player.AbstractPlayer;

import java.util.*;

public class AIPlayer extends AbstractPlayer
{
	public static final Logger LOGGER = LoggerFactory.getLogger(AIPlayer.class);
	private final BruteForceMethod bruteForceMethod;
	private final BruteForceMethod.Configuration configuration;

	private final UUID game;
	private MicroServiceScrabbleServer server; // TODO
	private Bag rack = Bag.builder().build();

//		private ComparatorSelector selector;

	public AIPlayer(final UUID game, final BruteForceMethod bruteForceMethod, final String name)
	{
		super(name);
		this.game = game;
		this.bruteForceMethod = bruteForceMethod;
//			super(new Configuration(), name);
		configuration = new BruteForceMethod.Configuration();
		configuration.strategy = new Strategy.BestScore();
		configuration.throttle = 2;
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

	/**
	 *
	 * @return a representation of this player.
	 */
	Player toData()
	{
		final Player player = Player.builder()
				.id(this.uuid)
				.name(this.name)
				.score(0)
				.rack(this.rack)
				.build();
		return player;
	}

}
