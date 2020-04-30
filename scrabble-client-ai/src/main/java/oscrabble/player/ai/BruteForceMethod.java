package oscrabble.player.ai;

import org.quinto.dawg.CompressedDAWGSet;
import org.quinto.dawg.DAWGNode;
import org.quinto.dawg.ModifiableDAWGSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.ScrabbleException;
import oscrabble.data.IDictionary;
import oscrabble.data.objects.Grid;
import oscrabble.server.IGame;

import java.io.*;
import java.util.*;

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


	Set<Grid.Square> getAnchors()
	{
		final LinkedHashSet<Grid.Square> anchors = new LinkedHashSet<>();

		if (this.grid.isEmpty())
		{
			throw new IllegalStateException("Cannot get anchors on an empty grid.");
		}

		for (final Grid.Square square : this.grid.getAllSquares())
		{
			if (!square.isEmpty())
			{
				for (final Grid.Square neighbour : square.getNeighbours())
				{
					if (neighbour.isEmpty())
					{
						anchors.add(neighbour);
					}
				}
			}
		}
		return anchors;
	}

	/**
	 * Get all authorized moves.
	 *
	 * @param rack Rack
	 * @return all the moves
	 */
	public Set<String> getLegalMoves(final String rack) 
	{
		if (this.grid.isEmpty())
		{
			return getLegalWordOnEmptyGrid(rack);
		}

		final CalculateCtx ctx = new CalculateCtx();
		ctx.grid = grid;
		ctx.rack = new LinkedList<>();
		for (final char c : rack.toCharArray())
		{
			ctx.rack.add(c);
		}

		final Set<Grid.Square> anchors = getAnchors();
		for (final Grid.Square anchor : anchors)
		{
			ctx.anchor = anchor;

			for (final Grid.Direction direction : Grid.Direction.values())
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
						partialWord.insert(0, square.c);
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

	private Set<String> getLegalWordOnEmptyGrid(final String rack)
	{
		if (!this.grid.isEmpty())
		{
			throw new IllegalStateException();
		}

		final List<Character> remaining = new LinkedList<>();
		for (final char c : rack.toCharArray())
		{
			remaining.add(c);
		}

		final Set<String> words = new HashSet<>();
		getWords(this.automaton.getSourceNode(), "", remaining, words);

		final Set<String> moves = new HashSet<>();
		final Grid.Square centralSquare = this.grid.getCentralSquare();
		for (final String word : words)
		{
			HashMap<Grid.Direction, Grid.Square> wordStart = new HashMap<>();
			wordStart.put(Grid.Direction.HORIZONTAL, centralSquare);
			wordStart.put(Grid.Direction.VERTICAL, centralSquare);

			for (int i = 0; i < word.length(); i++)
			{
				for (final Grid.Direction d: wordStart.keySet())
				{
					final Grid.Square startSquare = wordStart.get(d);
					moves.add(startSquare.getNotation(d) + " " + word);
					wordStart.put(d, startSquare.getPrevious(d));
				}
			}
		}
		return moves;
	}

	/**
	 * Collect the possible words from a given node.
	 *
	 * @param position current node
	 * @param reached word begin as it already has been computed
	 * @param remainingChars remaining chars
	 * @param collector bag to collect the results.
	 */
	private void getWords(final DAWGNode position, final String reached, final List<Character> remainingChars, final Set<String> collector)
	{
		if (remainingChars == null)
		{
			return;
		}

		for (final Character transition : getTransitions(position))
		{
			if (remainingChars.contains(transition))
			{
				remainingChars.remove((Character)transition);
				final String now = reached + transition;
				final DAWGNode newNode = position.transition(transition);
				if (newNode.isAcceptNode())
				{
					collector.add(now);
				}
				getWords(newNode, now, remainingChars, collector);
				remainingChars.add(transition);
			}
		}
	}

	private void leftPart(final CalculateCtx ctx, final String partialWord, final DAWGNode node, final int limit)
	{
		extendRight(ctx, partialWord, node, ctx.anchor);
		if (limit > 0)
		{
			for (final Character c : getTransitions(node))
			{
				if (ctx.rack.contains(c))
				{
					ctx.rack.remove((Character)c);
					leftPart(ctx, partialWord + c, node.transition(c), limit - 1);
					ctx.rack.add(c);
				}

				if (ctx.rack.contains(' '))
				{
					ctx.rack.remove((Character)' ');
					leftPart(ctx, partialWord + Character.toLowerCase(c), node.transition(c), limit - 1);
					ctx.rack.add(' ');
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
				char tile;
				if (allowedCrossCharacters.contains(letter))
				{
					if (ctx.rack.contains(' '))
					{
						tile = ' ';
						final DAWGNode nextNode = node.transition(letter);
						ctx.rack.remove((Character)tile);
						if (!possibleNextSquare.isBorder)
						{
							extendRight(ctx, partialWord + Character.toLowerCase(letter), nextNode,
									possibleNextSquare.getNext(ctx.direction));
						}
						ctx.rack.add(tile);
					}

					if (ctx.rack.contains(letter))
					{
						tile = letter;
						final DAWGNode nextNode = node.transition(letter);
						ctx.rack.remove((Character)tile);
						if (!possibleNextSquare.isBorder())
						{
							extendRight(ctx, partialWord + letter, nextNode,
									possibleNextSquare.getNext(ctx.direction));
						}
						ctx.rack.add(tile);
					}
				}
			}
		}
		else
		{
			final Character letter = possibleNextSquare.c;
			if (letter != null && getTransitions(node).contains(letter))
			{
				final DAWGNode nextNode = node.transition(letter);
				if (!possibleNextSquare.isLastOfLine(ctx.direction))
				{
					extendRight(ctx, partialWord + letter, nextNode, possibleNextSquare.getNext(ctx.direction));
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
		Grid.Square startSquare = endSquare;
		for (int i = 0; i < word.length() - 1; i++)
		{
			startSquare = startSquare.getPrevious(ctx.direction);
		}
		ctx.legalPlayTiles.add(
				Grid.Coordinate.getNotation(startSquare, ctx.direction) + " " + word
		);
	}

	private Set<Character> getAllowedCrossCharacters(final CalculateCtx ctx,
													 final Grid.Square crossSquare,
													 final Grid.Direction crossDirection)
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
				sb.insert(0, square.c);
				square = square.getPrevious(crossDirection);
			}

			final int emptySquare = sb.length();
			sb.append(" ");

			square = crossSquare.getNext(crossDirection);
			while (!square.isBorder() && !square.isEmpty())
			{
				sb.append(square.c);
				square = square.getNext(crossDirection);
			}

			final boolean allowAll = sb.length() == 1;

			for (char letter = 'A'; letter < 'Z'; letter++)
			{
				sb.setCharAt(emptySquare, letter);
				if (allowAll || this.automaton.contains(sb.toString()))
				{
					allowed.add(letter);
				}
			}
			crossChecks.put(crossSquare, allowed);
		}
		return crossChecks.get(crossSquare);
	}

	static class Configuration
	{
//		@Parameter(label = "#strategy")
		Strategy strategy = new Strategy.BestScore();

//		@Parameter(label = "#throttle.seconds", lowerBound = 0, upperBound = 30)
		int throttle = 2;

//		@Parameter(label = "#force", isSlide = true, lowerBound = 0, upperBound = 100)
		int force = 90;
	}

	static class CalculateCtx
	{
		Grid.Direction direction;
		Grid.Square anchor;
		Grid grid;

		List<Character> rack;
		Set<String> legalPlayTiles = new LinkedHashSet<>();

		final Map<Grid.Direction, Map<Grid.Square, Set<Character>>> crosschecks = new HashMap<>();
		{
			this.crosschecks.put(Grid.Direction.HORIZONTAL, new HashMap<>());
			this.crosschecks.put(Grid.Direction.VERTICAL, new HashMap<>());
		}
	}
}