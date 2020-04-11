package oscrabble.player;

import org.apache.log4j.Logger;
import org.quinto.dawg.CompressedDAWGSet;
import org.quinto.dawg.DAWGNode;
import org.quinto.dawg.ModifiableDAWGSet;
import oscrabble.*;
import oscrabble.action.PlayTiles;
import oscrabble.action.SkipTurn;
import oscrabble.configuration.ConfigurationPanel;
import oscrabble.configuration.Parameter;
import oscrabble.dictionary.Dictionary;
import oscrabble.server.Game;
import oscrabble.server.Play;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class BruteForceMethod
{
	private final static Logger LOGGER = Logger.getLogger(BruteForceMethod.class);

	CompressedDAWGSet automaton;

	public BruteForceMethod(final Dictionary dictionary)
	{
		loadDictionary(dictionary);
	}

	void loadDictionary(final Dictionary dictionary)
	{
		final File fff = new File("C:\\temp\\" + dictionary.getName() + "_" + dictionary.md5 + ".dawg");
		if (fff.exists())
		{
			try (ObjectInputStream fis = new ObjectInputStream(new FileInputStream(fff)))
			{
				this.automaton = ((CompressedDAWGSet) fis.readObject());
			}
			catch(final IOException | ClassNotFoundException e)
			{
				throw new IOError(e);
			}
		}
		else
		{
			final Set<String> mutations = new HashSet<>(dictionary.getMutations());

			// remove words with one letter
			final Iterator<String> it = mutations.iterator();
			it.forEachRemaining(w -> {
				if (w.length() == 1)
				{
					it.remove();
				}
			});

			this.automaton = new ModifiableDAWGSet(mutations).compress();
			try (ObjectOutputStream oss = new ObjectOutputStream(new FileOutputStream(fff)))
			{
				oss.writeObject(this.automaton);
			}
			catch (IOException e)
			{
				throw new IOError(e);
			}
		}
	}

	Set<Grid.Square> getAnchors(final Grid grid)
	{
		final LinkedHashSet<Grid.Square> anchors = new LinkedHashSet<>();

		if (grid.isEmpty())
		{
			// TODO: really treat this case
			anchors.add(grid.getCenter());
		}
		else
		{
			for (final Grid.Square square : grid.getAllSquares())
			{
				if (!square.isEmpty())
				{
					for (final Grid.Square neighbour : grid.getNeighbours(square))
					{
						if (neighbour.isEmpty())
						{
							anchors.add(neighbour);
						}
					}
				}
			}
		}
		return anchors;
	}

	/**
	 * Get all authorized moves.
	 *
	 * @param grid Grid
	 * @param rack Rack
	 * @return all the moves
	 */
	public Set<PlayTiles> getLegalMoves(final Grid grid, final Rack rack)
	{

		final CalculateCtx ctx = new CalculateCtx();
		ctx.grid = grid;
		ctx.rack = rack;

		final Set<Grid.Square> anchors = getAnchors(grid);
		for (final Grid.Square anchor : anchors)
		{
			ctx.anchor = anchor;

			for (final PlayTiles.Direction direction : PlayTiles.Direction.values())
			{
				ctx.direction = direction;
				final StringBuilder partialWord = new StringBuilder();
				DAWGNode node = this.automaton.getSourceNode();

				if (!anchor.isFirstOfLine(direction) && !anchor.getPrevious(direction).isEmpty())
				{
					Grid.Square square = anchor;
					do
					{
						square = square.getPrevious(direction);
						partialWord.insert(0, square.tile.getChar());
					} while (!square.isFirstOfLine(direction) && !square.getPrevious(direction).isEmpty());

					node = node.transition(partialWord.toString());
					extendRight(ctx, partialWord.toString(), node, anchor);
				}
				else
				{
					int nonAnchor = 0;
					Grid.Square square = anchor;
					while (!square.isFirstOfLine(direction) && !anchors.contains(square = square.getPrevious(direction)) && square.isEmpty())
					{
						nonAnchor++;
					}
					leftPart(ctx, "", node, nonAnchor);
				}
			}
		}

		LOGGER.info("" + ctx.legalPlayTiles.size() + " legal moves calculated");
		return ctx.legalPlayTiles;
	}

	private void leftPart(final CalculateCtx ctx, final String partialWord, final DAWGNode node, final int limit)
	{
		extendRight(ctx, partialWord, node, ctx.anchor);
		if (limit > 0)
		{
			for (final Character c : getTransitions(node))
			{
				Tile tile;
				tile = ctx.rack.searchLetter(c);
				if (tile != null)
				{
					ctx.rack.remove(tile);
					leftPart(ctx, partialWord + c, node.transition(c), limit - 1);
					ctx.rack.add(tile);
				}

				tile = ctx.rack.searchLetter(' ');
				if (tile != null)
				{
					ctx.rack.remove(tile);
					leftPart(ctx, partialWord + Character.toLowerCase(c), node.transition(c), limit - 1);
					ctx.rack.add(tile);
				}
			}
		}
	}

	private void extendRight(final CalculateCtx ctx,
							 final String partialWord,
							 final DAWGNode node,
							 final Grid.Square possibleNextSquare)
	{

		if (
				(possibleNextSquare.isEmpty() || possibleNextSquare.isBorder())
						&& node.isAcceptNode()
						&& possibleNextSquare != ctx.anchor
		)
		{
			addLegalMove(ctx, possibleNextSquare.getPrevious(ctx.direction), partialWord);
		}

		if (possibleNextSquare.isBorder())
		{
			return;
		}

		if (possibleNextSquare.isEmpty())
		{
			final Set<Character> allowedCrossCharacters = getAllowedCrossCharacters(
					ctx,
					possibleNextSquare,
					ctx.direction.other()
			);

			for (final Character letter : getTransitions(node))
			{
				Tile tile;
				if (allowedCrossCharacters.contains(letter))
				{
					if ((tile = ctx.rack.searchLetter(' ')) != null)
					{
						final DAWGNode nextNode = node.transition(letter);
						ctx.rack.remove(tile);
						if (!possibleNextSquare.isLastOfLine(ctx.direction))
						{
							extendRight(ctx, partialWord + Character.toLowerCase(letter), nextNode,
									possibleNextSquare.getFollowing(ctx.direction));
						}
						ctx.rack.add(tile);
					}

					if ((tile = ctx.rack.searchLetter(letter)) != null)
					{
						final DAWGNode nextNode = node.transition(letter);
						ctx.rack.remove(tile);
						if (!possibleNextSquare.isLastOfLine(ctx.direction))
						{
							extendRight(ctx, partialWord + letter, nextNode,
									possibleNextSquare.getFollowing(ctx.direction));
						}
						ctx.rack.add(tile);
					}
				}
			}
		}
		else
		{
			final char letter = possibleNextSquare.tile.getChar();
			if (getTransitions(node).contains(letter))
			{
				final DAWGNode nextNode = node.transition(letter);
				if (!possibleNextSquare.isLastOfLine(ctx.direction))
				{
					extendRight(ctx, partialWord + letter, nextNode, possibleNextSquare.getFollowing(ctx.direction));
				}
			}
		}

	}

	static private Set<Character> getTransitions(final DAWGNode node)
	{
		final LinkedHashSet<Character> transitions = new LinkedHashSet<>();
		for (char c = 'A'; c <= 'Z'; c++)
		{
			if (node.transition(c) != null)
			{
				transitions.add(c);
			}
		}
		return transitions;
	}

	private void addLegalMove(final CalculateCtx ctx, final Grid.Square endSquare, final String word)
	{
		final int delta = word.length() - 1;
		final int wx = endSquare.getX() - (ctx.direction == PlayTiles.Direction.HORIZONTAL ? delta : 0);
		final int wy = endSquare.getY() - (ctx.direction == PlayTiles.Direction.VERTICAL ? delta : 0);
		ctx.legalPlayTiles.add(new PlayTiles(
				ctx.grid.getSquare(wx, wy),
				ctx.direction,
				word)
		);
	}

	private Set<Character> getAllowedCrossCharacters(final CalculateCtx ctx,
													 final Grid.Square crossSquare,
													 final PlayTiles.Direction crossDirection)
	{
		if (!crossSquare.isEmpty())
		{
			throw new IllegalStateException("Should not be called on occupied square");
		}

		final Map<Grid.Square, Set<Character>> crossChecks = ctx.crosschecks.get(crossDirection);
		if (!crossChecks.containsKey(crossSquare))
		{
			final TreeSet<Character> allowed = new TreeSet<>();


			final StringBuilder sb = new StringBuilder();

			Grid.Square square = crossSquare.getPrevious(crossDirection);
			while (!square.isBorder() && !square.isEmpty())
			{
				sb.insert(0, square.tile.getChar());
				square = square.getPrevious(crossDirection);
			}

			final int emptyPosition = sb.length();
			sb.append(" ");

			square = crossSquare.getFollowing(crossDirection);
			while (!square.isBorder() && !square.isEmpty())
			{
				sb.append(square.tile.getChar());
				square = square.getFollowing(crossDirection);
			}

			final boolean allowAll = sb.length() == 1;

			for (char letter = 'A'; letter < 'Z'; letter++)
			{
				sb.setCharAt(emptyPosition, letter);
				if (allowAll || this.automaton.contains(sb.toString()))
				{
					allowed.add(letter);
				}
			}
			crossChecks.put(crossSquare, allowed);
		}
		return crossChecks.get(crossSquare);
	}

	public class Player extends AbstractPlayer
	{

		private ComparatorSelector selector;

		public Player(final String name)
		{
			super(new Configuration(), name);
		}

		@Override
		public void onPlayRequired(final Play play)
		{
			if (play.player != this)
			{
				return;
			}

			try
			{
				final Rack rack = this.game.getRack(this, this.playerKey);
				Set<PlayTiles> playTiles = new HashSet<>(getLegalMoves(
						this.game.getGrid(), rack));

				if (playTiles.isEmpty())
				{
					this.game.sendMessage(this, "No possible moves anymore");
					this.game.play(this.playerKey, play, SkipTurn.SINGLETON);
				}
				else
				{
					final Configuration configuration = this.getConfiguration();
					if (configuration.throttle > 0)
					{
						LOGGER.trace("Wait " + configuration.throttle + " seconds...");
						Thread.sleep(configuration.throttle * 1000);
					}
					final PlayTiles toPlay = this.selector.select(playTiles);
					if (this.game.getPlayerToPlay().equals(this))  // check the player still is on turn and no rollback toke place.
					{
						LOGGER.info("Play " + toPlay);
						this.game.play(this.playerKey, play, toPlay);
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

		@Override
		public void onDispatchMessage(String msg)
		{
			LOGGER.debug("Received message: " + msg);
		}

		@Override
		public void afterPlay(final Play play)
		{
			// nichts
		}

		@Override
		public void beforeGameStart()
		{
			updateConfiguration();
		}

		@Override
		public boolean isObserver()
		{
			return false;
		}

		@Override
		public String toString()
		{
			return this.getName();
		}

		@Override
		public boolean hasEditableParameters()
		{
			return true;
		}

		@Override
		public void editParameters()
		{

			final JPanel panel = new JPanel();
			panel.setBorder(new TitledBorder("Parameters"));
			panel.add(new ConfigurationPanel(this.configuration));

			final JScrollPane sp = new JScrollPane(panel);
			sp.setBorder(null);
			JOptionPane.showOptionDialog(
					null,
					sp,
					"Options for " + getName(),
					JOptionPane.DEFAULT_OPTION,
					JOptionPane.PLAIN_MESSAGE,
					null,
					null,
					null
			);

			updateConfiguration();
		}

		@Override
		public Game.PlayerType getType()
		{
			return Game.PlayerType.BRUTE_FORCE;
		}

		protected void updateConfiguration()
		{
			final Supplier<Grid> gridSupplier = () -> Player.this.game.getGrid();
			final Configuration configuration = getConfiguration();
			this.selector = new ComparatorSelector(gridSupplier, configuration.strategy.valuator);
			this.selector.setMean(configuration.force / 100f);
		}

		/**
		 * Load or update the configuration from a properties set.
		 * @param properties properties to configure the player with.
		 */
		public void loadConfiguration(final Properties properties)
		{
			this.configuration.loadProperties(properties);
		}

		@Override
		public Configuration getConfiguration()
		{
			return (Configuration) this.configuration;
		}
	}

	static class Configuration extends oscrabble.configuration.Configuration
	{
		@Parameter(label = "#strategy")
		Strategy strategy = Strategy.BEST_SCORE;

		@Parameter(label = "#throttle.seconds", lowerBound = 0, upperBound = 30)
		int throttle = 2;

		@Parameter(label = "#force", isSlide = true, lowerBound = 0, upperBound = 100)
		int force = 90;
	}

	static class CalculateCtx
	{
		PlayTiles.Direction direction;
		Grid.Square anchor;
		Grid grid;

		Rack rack;
		Set<PlayTiles> legalPlayTiles = new LinkedHashSet<>();

		final Map<PlayTiles.Direction, Map<Grid.Square, Set<Character>>> crosschecks = new HashMap<>();
		{
			this.crosschecks.put(PlayTiles.Direction.HORIZONTAL, new HashMap<>());
			this.crosschecks.put(PlayTiles.Direction.VERTICAL, new HashMap<>());
		}
	}


	/**
	 * Playing strategy for a player
	 */
	@SuppressWarnings("unused")
	public enum Strategy
	{
		BEST_SCORE("Best score", mi -> mi.getScore()),
		MAX_LENGTH("Max length", mi -> mi.getRequiredLetters().size());

		private final String label;
		private final Function<Grid.MoveMetaInformation, Integer> valuator;

		Strategy(final String label, Function<Grid.MoveMetaInformation, Integer> valuator)
		{
			this.label = label;
			this.valuator = valuator;
		}

		@Override
		public String toString()
		{
			return this.label;
		}
	}


}