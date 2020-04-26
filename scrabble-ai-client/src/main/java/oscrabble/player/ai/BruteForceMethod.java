package oscrabble.player.ai;

import org.quinto.dawg.CompressedDAWGSet;
import org.quinto.dawg.DAWGNode;
import org.quinto.dawg.ModifiableDAWGSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.ScrabbleException;
import oscrabble.configuration.Parameter;
import oscrabble.data.IDictionary;
import oscrabble.data.MessageFromServer;
import oscrabble.server.Game;
import oscrabble.server.Grid;
import oscrabble.server.IGame;
import oscrabble.server.action.Action;

import java.io.*;
import java.util.*;
import java.util.function.Function;

public class BruteForceMethod
{
	private final static Logger LOGGER = LoggerFactory.getLogger(BruteForceMethod.class);
	private final IDictionary dictionary;

	CompressedDAWGSet automaton;
	
	/** The grid, to update after each round */
	Grid grid;

	private final int gridSize;

	public BruteForceMethod(final IDictionary dictionary)
	{
		this.dictionary = dictionary;
		loadDictionary(dictionary);
		this.gridSize = dictionary.getScrabbleRules().gridSize;
	}

	void loadDictionary(final IDictionary dictionary)
	{
		final Set<String> admissibleWords = new HashSet<>(dictionary.getAdmissibleWords());
		final File fff = new File("C:\\temp\\scrabble_dawg_" + admissibleWords.hashCode() + ".dawg");
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
			// remove words with one letter
			final Iterator<String> it = admissibleWords.iterator();
			it.forEachRemaining(w -> {
				if (w.length() == 1)
				{
					it.remove();
				}
			});

			this.automaton = new ModifiableDAWGSet(admissibleWords).compress();
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

	Set<Position> getAnchors() throws ScrabbleException.ForbiddenPlayException
	{
		final LinkedHashSet<Position> anchors = new LinkedHashSet<>();

		if (grid.isEmpty())
		{
			// TODO: really treat this case
			final int center = (int) Math.ceil(this.dictionary.getScrabbleRules().gridSize / 2f);
			anchors.add(new Position(center, center));
		}
		else
		{
			for (final Position square : getAllSquares())
			{
				if (!square.isEmpty())
				{
					for (final Position neighbour : square.getNeighbours())
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
	
	private List<Position> getAllSquares()
	{
		final ArrayList<Position> positions = new ArrayList<>();
		final int gridSize = this.dictionary.getScrabbleRules().gridSize;
		for (int x = 0; x < gridSize; x++)
		{
			for (int y = 0; y < gridSize; y++)
			{
				positions.add(new Position(x, y));
			}
		}
		return positions;
	}

	/**
	 * Get all authorized moves.
	 *
	 * @param rack Rack
	 * @return all the moves
	 */
	public Set<String> getLegalMoves(final String rack) throws ScrabbleException.ForbiddenPlayException
	{

		final CalculateCtx ctx = new CalculateCtx();
		ctx.grid = grid;
		ctx.rack = new LinkedList<>();
		for (final char c : rack.toCharArray())
		{
			ctx.rack.add(c);
		}

		final Set<Position> anchors = getAnchors();
		for (final Position anchor : anchors)
		{
			ctx.anchor = anchor;

			for (final Action.Direction direction : Action.Direction.values())
			{
				ctx.direction = direction;
				final StringBuilder partialWord = new StringBuilder();
				DAWGNode node = this.automaton.getSourceNode();

				if (!anchor.isFirstOfLine(direction) && !anchor.getPrevious(direction).isEmpty())
				{
					Position square = anchor;
					do
					{
						square = square.getPrevious(direction);
						partialWord.insert(0, square.getChar());
					} while (!square.isFirstOfLine(direction) && !square.getPrevious(direction).isEmpty());

					node = node.transition(partialWord.toString());
					extendRight(ctx, partialWord.toString(), node, anchor);
				}
				else
				{
					int nonAnchor = 0;
					Position square = anchor;
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

	private void leftPart(final CalculateCtx ctx, final String partialWord, final DAWGNode node, final int limit) throws ScrabbleException.ForbiddenPlayException
	{
		extendRight(ctx, partialWord, node, ctx.anchor);
		if (limit > 0)
		{
			for (final Character c : getTransitions(node))
			{
				String tile;
				if (ctx.rack.contains(c))
				{
					ctx.rack.remove(c);
					leftPart(ctx, partialWord + c, node.transition(c), limit - 1);
					ctx.rack.add(c);
				}

				if (ctx.rack.contains(' '))
				{
					ctx.rack.remove(' ');
					leftPart(ctx, partialWord + Character.toLowerCase(c), node.transition(c), limit - 1);
					ctx.rack.add(' ');
				}
			}
		}
	}

	private void extendRight(final CalculateCtx ctx,
							 final String partialWord,
							 final DAWGNode node,
							 final Position possibleNextSquare) throws ScrabbleException.ForbiddenPlayException
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
				char tile;
				if (allowedCrossCharacters.contains(letter))
				{
					if (ctx.rack.contains(' '))
					{
						tile = ' ';
						final DAWGNode nextNode = node.transition(letter);
						ctx.rack.remove(tile);
						if (!possibleNextSquare.isLastOfLine(ctx.direction))
						{
							extendRight(ctx, partialWord + Character.toLowerCase(letter), nextNode,
									possibleNextSquare.getFollowing(ctx.direction));
						}
						ctx.rack.add(tile);
					}

					if (ctx.rack.contains(letter))
					{
						tile = letter;
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
			final Character letter = possibleNextSquare.getChar();
			if (letter != null && getTransitions(node).contains(letter))
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

	private void addLegalMove(final CalculateCtx ctx, final Position endSquare, final String word)
	{
		Position startSquare = endSquare;
		for (int i = 0; i < word.length() - 1; i++)
		{
			startSquare = startSquare.getPrevious(ctx.direction);
		}
		ctx.legalPlayTiles.add(
				startSquare.getNotation(ctx.direction) + " " + word
		);
	}

	private Set<Character> getAllowedCrossCharacters(final CalculateCtx ctx,
													 final Position crossSquare,
													 final Action.Direction crossDirection) throws ScrabbleException.ForbiddenPlayException
	{
		if (!crossSquare.isEmpty())
		{
			throw new IllegalStateException("Should not be called on occupied square");
		}

		final Map<Position, Set<Character>> crossChecks = ctx.crosschecks.get(crossDirection);
		if (!crossChecks.containsKey(crossSquare))
		{
			final TreeSet<Character> allowed = new TreeSet<>();


			final StringBuilder sb = new StringBuilder();

			Position square = crossSquare.getPrevious(crossDirection);
			while (!square.isBorder() && !square.isEmpty())
			{
				sb.insert(0, square.getChar());
				square = square.getPrevious(crossDirection);
			}

			final int emptyPosition = sb.length();
			sb.append(" ");

			square = crossSquare.getFollowing(crossDirection);
			while (!square.isBorder() && !square.isEmpty())
			{
				sb.append(square.getChar());
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

	public class Player extends oscrabble.data.Player
	{
		private final Configuration configuration;
		private IGame game; // TODO

//		private ComparatorSelector selector;

		public Player(final String name)
		{
//			super(new Configuration(), name);
			configuration = new Configuration();
			configuration.strategy = Strategy.BEST_SCORE;
			configuration.throttle = 2;
			this.name = name;
		}

		public void onPlayRequired(final Player player)
		{
			if (player != Player.this)
			{
				return;
			}

			try
			{
				final String rack = this.game.getRack(this);
				Set<String> possibleMoves = new HashSet<>(getLegalMoves(rack));

				if (possibleMoves.isEmpty())
				{
					this.game.sendMessage(this, "No possible moves anymore");
					this.game.play(Action.parse("-"));
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
					if (this.game.getPlayerToPlay().equals(this))  // check the player still is on turn and no rollback toke place.
					{
						LOGGER.info("Play " + sortedMoves.getFirst());
						this.game.play(Action.parse(sortedMoves.getFirst()));
					}
				}
			}
			catch (ScrabbleException | oscrabble.data.ScrabbleException e)
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
		Action.Direction direction;
		Position anchor;
		Grid grid;

		List<Character> rack;
		Set<String> legalPlayTiles = new LinkedHashSet<>();

		final Map<Action.Direction, Map<Position, Set<Character>>> crosschecks = new HashMap<>();
		{
			this.crosschecks.put(Action.Direction.HORIZONTAL, new HashMap<>());
			this.crosschecks.put(Action.Direction.VERTICAL, new HashMap<>());
		}
	}


	/**
	 * Playing strategy for a player
	 */
	@SuppressWarnings("unused")
	public enum Strategy
	{
		BEST_SCORE("Best score", mi -> mi.getScore()),
		MAX_LENGTH("Max length", mi -> mi.requiredLetter.size());

		private final String label;
		private final Function<Grid.MoveMetaInformation, Integer> valuator;

		Strategy(final String label, Function<Grid.MoveMetaInformation, Integer> valuator)
		{
			this.label = label;
			this.valuator = valuator;
		}

		/**
		 * Sort a list of moves, the better the first.
		 *
		 * @param moves
		 */
		final void sort(final List<String> moves)
		{
			// TODO
		}

		@Override
		public String toString()
		{
			return this.label;
		}
	}


	private class Position 
	{
		final int x;
		final int y;
		
		public Position(final int x, final int y)
		{
			this.x = x;
			this.y = y;
		}
		
		boolean isEmpty() throws ScrabbleException.ForbiddenPlayException
		{
			return grid.isEmpty(getNotation(Action.Direction.HORIZONTAL));
		}

		private String getNotation(final Action.Direction direction)
		{
			final String xPart = Integer.toString(x + 1);
			final String yPart = Character.toString((char) ('A' + y));
			return direction ==  Action.Direction.HORIZONTAL
					? xPart + yPart
					: yPart + xPart;
		}

		public List<Position> getNeighbours()
		{
			final int gridSize = dictionary.getScrabbleRules().gridSize;
			final ArrayList<Position> neighbours = new ArrayList<>(4);
			if (x>0) neighbours.add(new Position(x - 1, y));
			if (y>0) neighbours.add(new Position(x, y - 1));
			if (x< gridSize-1) neighbours.add(new Position(x + 1, y));
			if (y<gridSize-1) neighbours.add(new Position(x, y + 1));
			return neighbours;
		}

		public boolean isFirstOfLine(final Action.Direction direction)
		{
			return
					(direction == Action.Direction.HORIZONTAL && x == 0)
					|| (direction == Action.Direction.VERTICAL && y == 0);
		}

		public Position getPrevious(final Action.Direction direction)
		{
			return new Position(
					direction == Action.Direction.HORIZONTAL ? x - 1 : x,
					direction == Action.Direction.VERTICAL ? y - 1 : y
			);
		}

		public Character getChar() throws ScrabbleException.ForbiddenPlayException
		{
			return grid.getChar(getNotation(Action.Direction.HORIZONTAL));
		}

		public boolean isBorder()
		{
			return x == 0 || y == 0 || x == gridSize - 1 || y == gridSize - 1;
		}

		public boolean isLastOfLine(final Action.Direction direction)
		{
			return (direction == Action.Direction.HORIZONTAL && x == gridSize - 1)
					|| (direction == Action.Direction.VERTICAL && y == gridSize - 1);
		}

		public Position getFollowing(final Action.Direction direction)
		{
			return new Position(
					direction == Action.Direction.HORIZONTAL ? x + 1 : x,
					direction == Action.Direction.VERTICAL ? y + 1 : y
			);
		}
	}
}