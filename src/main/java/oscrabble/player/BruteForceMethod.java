package oscrabble.player;

import org.apache.commons.collections4.bidimap.DualLinkedHashBidiMap;
import org.apache.log4j.Logger;
import org.quinto.dawg.CompressedDAWGSet;
import org.quinto.dawg.DAWGNode;
import org.quinto.dawg.ModifiableDAWGSet;
import oscrabble.*;
import oscrabble.dictionary.Dictionary;
import oscrabble.server.IAction;
import oscrabble.server.IPlayerInfo;
import oscrabble.server.IScrabbleServer;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;

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
			this.automaton = new ModifiableDAWGSet(dictionary.getMutations()).compress();
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
	public Set<Move> getLegalMoves(final Grid grid, final Rack rack)
	{

		final CalculateCtx ctx = new CalculateCtx();
		ctx.grid = grid;
		ctx.rack = rack;

		final Set<Grid.Square> anchors = getAnchors(grid);
		for (final Grid.Square anchor : anchors)
		{
			ctx.anchor = anchor;

			for (final Move.Direction direction : Move.Direction.values())
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
						partialWord.insert(0, square.stone.getChar());
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

		LOGGER.info("" + ctx.legalMoves.size() + " legal moves calculated");
		return ctx.legalMoves;
	}

	private void leftPart(final CalculateCtx ctx, final String partialWord, final DAWGNode node, final int limit)
	{
		extendRight(ctx, partialWord, node, ctx.anchor);
		if (limit > 0)
		{
			for (final Character c : getTransitions(node))
			{
				Stone stone;
				stone = ctx.rack.searchLetter(c);
				if (stone != null)
				{
					ctx.rack.remove(stone);
					leftPart(ctx, partialWord + c, node.transition(c), limit - 1);
					ctx.rack.add(stone);
				}

				stone = ctx.rack.searchLetter(' ');
				if (stone != null)
				{
					ctx.rack.remove(stone);
					leftPart(ctx, partialWord + Character.toLowerCase(c), node.transition(c), limit - 1);
					ctx.rack.add(stone);
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
				Stone stone;
				if (allowedCrossCharacters.contains(letter))
				{
					if ((stone = ctx.rack.searchLetter(' ')) != null)
					{
						final DAWGNode nextNode = node.transition(letter);
						ctx.rack.remove(stone);
						if (!possibleNextSquare.isLastOfLine(ctx.direction))
						{
							extendRight(ctx, partialWord + Character.toLowerCase(letter), nextNode,
									possibleNextSquare.getFollowing(ctx.direction));
						}
						ctx.rack.add(stone);
					}

					if ((stone = ctx.rack.searchLetter(letter)) != null)
					{
						final DAWGNode nextNode = node.transition(letter);
						ctx.rack.remove(stone);
						if (!possibleNextSquare.isLastOfLine(ctx.direction))
						{
							extendRight(ctx, partialWord + letter, nextNode,
									possibleNextSquare.getFollowing(ctx.direction));
						}
						ctx.rack.add(stone);
					}
				}
			}
		}
		else
		{
			final char letter = possibleNextSquare.stone.getChar();
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
		final int wx = endSquare.getX() - (ctx.direction == Move.Direction.HORIZONTAL ? delta : 0);
		final int wy = endSquare.getY() - (ctx.direction == Move.Direction.VERTICAL ? delta : 0);
		ctx.legalMoves.add(new Move(
				ctx.grid.getSquare(wx, wy),
				ctx.direction,
				word)
		);
	}

	private Set<Character> getAllowedCrossCharacters(final CalculateCtx ctx,
													 final Grid.Square crossSquare,
													 final Move.Direction crossDirection)
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
				sb.insert(0, square.stone.getChar());
				square = square.getPrevious(crossDirection);
			}

			final int emptyPosition = sb.length();
			sb.append(" ");

			square = crossSquare.getFollowing(crossDirection);
			while (!square.isBorder() && !square.isEmpty())
			{
				sb.append(square.stone.getChar());
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
		private final IScrabbleServer server;
		private final LinkedHashMap<String, IMoveSelector> possibleMoveSelectors;
		private IMoveSelector selectionMethod;

		public Player(final IScrabbleServer server, final String name)
		{
			super(name);
			this.server = server;
			this.possibleMoveSelectors = new LinkedHashMap<>();
			this.possibleMoveSelectors.put("Best score", new ComparatorSelector(server.getGrid(), Grid.MoveMetaInformation.SCORE_COMPARATOR));
			this.possibleMoveSelectors.put("Longest word", new ComparatorSelector(server.getGrid(), Grid.MoveMetaInformation.WORD_LENGTH_COMPARATOR));
			this.selectionMethod = this.possibleMoveSelectors.values().iterator().next();
		}

		@Override
		public void onPlayRequired(final AbstractPlayer currentPlayer)
		{
			if (currentPlayer != this)
			{
				return;
			}

			try
			{
				Set<Move> moves = new HashSet<>(getLegalMoves(
						this.server.getGrid(), this.server.getRack(this, this.playerKey)));

				if (moves.isEmpty())
				{
					// TODO - passt oder ersetzt manche Stones
					throw new UnsupportedOperationException();
				}
				else
				{
					final Move toPlay = this.selectionMethod.select(moves);
					LOGGER.info("Play " + toPlay);
					this.server.play(this, toPlay);
				}
			}
			catch (ScrabbleException e)
			{
				throw new Error(e);
			}
		}

		@Override
		public void onDictionaryChange()
		{
			loadDictionary(this.server.getDictionary());
		}

		@Override
		public void onDispatchMessage(String msg)
		{
			System.out.println(msg);
		}

		@Override
		public void afterPlay(final IPlayerInfo player, final IAction action, final int score)
		{
			// nichts
		}

		@Override
		public void beforeGameStart()
		{

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
			panel.setLayout(new GridLayout(0, 2));
			panel.add(new JLabel("Strategie"));
			final JComboBox<String> cb = new JComboBox<>();
			this.possibleMoveSelectors.entrySet().forEach(enty -> {
				cb.addItem(enty.getKey());
				if (enty.getValue() == this.selectionMethod)
				{
					cb.setSelectedItem(enty.getKey());
				}
			});
			panel.add(cb);
			final int ok = JOptionPane.showOptionDialog(
					null,
					new JScrollPane(panel),
					"Options for " + getName(),
					JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.PLAIN_MESSAGE,
					null,
					null,
					null
					);

			if (ok == JOptionPane.OK_OPTION)
			{
				this.selectionMethod = this.possibleMoveSelectors.get(cb.getModel().getSelectedItem());
			}
		}
	}

	static class CalculateCtx
	{
		Move.Direction direction;
		Grid.Square anchor;
		Grid grid;

		Rack rack;
		Set<Move> legalMoves = new LinkedHashSet<>();

		final Map<Move.Direction, Map<Grid.Square, Set<Character>>> crosschecks = new HashMap<>();
		{
			this.crosschecks.put(Move.Direction.HORIZONTAL, new HashMap<>());
			this.crosschecks.put(Move.Direction.VERTICAL, new HashMap<>());
		}
	}

}
