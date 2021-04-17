package oscrabble.player.ai;

import org.apache.commons.io.FileUtils;
import org.quinto.dawg.CompressedDAWGSet;
import org.quinto.dawg.DAWGNode;
import org.quinto.dawg.ModifiableDAWGSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.data.IDictionary;
import oscrabble.data.objects.Coordinate;
import oscrabble.data.objects.Grid;
import oscrabble.data.objects.Square;

import java.io.*;
import java.util.*;

public class BruteForceMethod {
	private final static Logger LOGGER = LoggerFactory.getLogger(BruteForceMethod.class);

	CompressedDAWGSet automaton;

	/**
	 * The grid, to update after each round
	 */
	Grid grid;

	public BruteForceMethod(final IDictionary dictionary) {
		loadDictionary(dictionary);
	}

	static private Set<Character> getTransitions(final DAWGNode node) {
		final LinkedHashSet<Character> transitions = new LinkedHashSet<>();
		for (char c = 'A'; c <= 'Z'; c++) {
			if (node.transition(c) != null) {
				transitions.add(c);
			}
		}
		return transitions;
	}

	void loadDictionary(final IDictionary dictionary) {
		final Set<String> admissibleWords = new HashSet<>(dictionary.getAdmissibleWords());
		final File fff = new File(
				FileUtils.getTempDirectory(),
				"scrabble_dawg_" + admissibleWords.hashCode() + ".dawg"
		);
		if (fff.exists()) {
			try (ObjectInputStream fis = new ObjectInputStream(new FileInputStream(fff))) {
				this.automaton = ((CompressedDAWGSet) fis.readObject());
			} catch (final IOException | ClassNotFoundException e) {
				throw new IOError(e);
			}
		} else {
			// remove words with one letter
			final Iterator<String> it = admissibleWords.iterator();
			it.forEachRemaining(w -> {
				if (w.length() == 1) {
					it.remove();
				}
			});

			this.automaton = new ModifiableDAWGSet(admissibleWords).compress();
			try (ObjectOutputStream oss = new ObjectOutputStream(new FileOutputStream(fff))) {
				oss.writeObject(this.automaton);
			} catch (IOException e) {
				throw new IOError(e);
			}
		}
	}

	public void setGrid(final Grid grid) {
		this.grid = grid;
	}

	Set<Square> getAnchors() {
		final LinkedHashSet<Square> anchors = new LinkedHashSet<>();

		if (this.grid.isEmpty()) {
			throw new IllegalStateException("Cannot get anchors on an empty grid.");
		}

		for (final Square square : this.grid.getAllSquares()) {
			if (!square.isEmpty()) {
				for (final Square neighbour : this.grid.getNeighbours(square)) {
					if (neighbour.isEmpty()) {
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
	public Set<String> getLegalMoves(final Collection<Character> rack) {
		if (this.grid.isEmpty()) {
			return getLegalWordOnEmptyGrid(rack);
		}

		final CalculateCtx ctx = new CalculateCtx();
		ctx.grid = this.grid;
		ctx.rack = new LinkedList<>();
		ctx.rack.addAll(rack);

		final Set<Square> anchors = getAnchors();
		for (final Square anchor : anchors) {
			ctx.anchor = anchor;

			for (final Grid.Direction direction : Grid.Direction.values()) {
				ctx.direction = direction;
				final StringBuilder partialWord = new StringBuilder();

				if (!anchor.isFirstOfLine(direction) && !this.grid.getPrevious(anchor, direction).isEmpty()) {
					DAWGNode node = this.automaton.getSourceNode();
					Square square = anchor;
					do {
						square = this.grid.getPrevious(square, direction);
						partialWord.insert(0, Character.toUpperCase(square.tile.c));
					} while (!square.isFirstOfLine(direction) && !this.grid.getPrevious(square, direction).isEmpty());

					node = node.transition(partialWord.toString());
					extendRight(ctx, partialWord.toString(), node, anchor);
				} else {
					int nonAnchor = 0;
					Square square = anchor;
					while (!square.isFirstOfLine(direction) && !anchors.contains(square = this.grid.getPrevious(square, direction)) && square.isEmpty()) {
						nonAnchor++;
					}
					leftPart(ctx, "", this.automaton.getSourceNode(), nonAnchor);
				}
			}
		}

		LOGGER.debug("" + ctx.legalPlayTiles.size() + " legal moves calculated");
		return ctx.legalPlayTiles;
	}

	private Set<String> getLegalWordOnEmptyGrid(final Collection<Character> rack) {
		if (!this.grid.isEmpty()) {
			throw new IllegalStateException();
		}

		final List<Character> remaining = new LinkedList<>(rack);

		final Set<String> words = new HashSet<>();
		getWords(this.automaton.getSourceNode(), "", remaining, words);

		final Set<String> moves = new HashSet<>();
		final Square centralSquare = this.grid.getCentralSquare();
		for (final String word : words) {
			HashMap<Grid.Direction, Square> wordStart = new HashMap<>();
			wordStart.put(Grid.Direction.HORIZONTAL, centralSquare);
			wordStart.put(Grid.Direction.VERTICAL, centralSquare);

			for (int i = 0; i < word.length(); i++) {
				for (final Grid.Direction d : wordStart.keySet()) {
					final Square startSquare = wordStart.get(d);
					moves.add(startSquare.getNotation(d) + " " + word);
					wordStart.put(d, this.grid.getPrevious(startSquare, d));
				}
			}
		}
		return moves;
	}

	/**
	 * Collect the possible words from a given node.
	 *
	 * @param position       current node
	 * @param reached        word begin as it already has been computed
	 * @param remainingChars remaining chars
	 * @param collector      bag to collect the results.
	 */
	private void getWords(final DAWGNode position, final String reached, final List<Character> remainingChars, final Set<String> collector) {
		if (remainingChars == null) {
			return;
		}

		for (final Character transition : getTransitions(position)) {
			if (remainingChars.contains(transition)) {
				//noinspection RedundantCast
				remainingChars.remove((Character) transition);
				final String now = reached + transition;
				final DAWGNode newNode = position.transition(transition);
				if (newNode.isAcceptNode()) {
					collector.add(now);
				}
				getWords(newNode, now, remainingChars, collector);
				remainingChars.add(transition);
			}
		}
	}

	/**
	 * Collect the words accepted by the grid with a given anchor.
	 *
	 * @param ctx         the context
	 * @param partialWord start of the word
	 * @param node        current node
	 * @param limit
	 */
	private void leftPart(final CalculateCtx ctx, final String partialWord, final DAWGNode node, final int limit) {
		extendRight(ctx, partialWord, node, ctx.anchor);
		if (limit > 0) {
			for (final Character c : getTransitions(node)) {
				if (ctx.rack.contains(c)) {
					//noinspection RedundantCast
					ctx.rack.remove((Character) c);
					leftPart(ctx, partialWord + c, node.transition(c), limit - 1);
					ctx.rack.add(c);
				}

				if (ctx.rack.contains(' ')) {
					ctx.rack.remove((Character) ' ');
					leftPart(ctx, partialWord + Character.toLowerCase(c), node.transition(c), limit - 1);
					ctx.rack.add(' ');
				}
			}
		}
	}

	/**
	 * For a given square and given word start, collect all correct words the grid allows with this word start.
	 *
	 * @param ctx
	 * @param partialWord
	 * @param node
	 * @param possibleNextSquare
	 */
	private void extendRight(final CalculateCtx ctx,
							 final String partialWord,
							 final DAWGNode node,
							 final Square possibleNextSquare
	) {

		if (
				(possibleNextSquare.isEmpty() || possibleNextSquare.isBorder())
						&& node.isAcceptNode()
						&& possibleNextSquare != ctx.anchor
		) {
			addLegalMove(ctx, this.grid.getPrevious(possibleNextSquare, ctx.direction), partialWord);
		}

		if (possibleNextSquare.isBorder()) {
			return;
		}

		if (possibleNextSquare.isEmpty()) {
			final Set<Character> allowedCrossCharacters = getAllowedCrossCharacters(
					ctx,
					possibleNextSquare,
					ctx.direction.other()
			);

			for (final Character letter : getTransitions(node)) {
				char tile;
				if (allowedCrossCharacters.contains(letter)) {
					if (ctx.rack.contains(' ')) {
						tile = ' ';
						final DAWGNode nextNode = node.transition(letter);
						ctx.rack.remove((Character) tile);
						if (!possibleNextSquare.isBorder) {
							extendRight(ctx, partialWord + Character.toLowerCase(letter), nextNode,
									this.grid.getNext(possibleNextSquare, ctx.direction));
						}
						ctx.rack.add(tile);
					}

					if (ctx.rack.contains(letter)) {
						tile = letter;
						final DAWGNode nextNode = node.transition(letter);
						ctx.rack.remove((Character) tile);
						if (!possibleNextSquare.isBorder()) {
							extendRight(ctx, partialWord + letter, nextNode,
									this.grid.getNext(possibleNextSquare, ctx.direction));
						}
						ctx.rack.add(tile);
					}
				}
			}
		} else {
			final Character letter = possibleNextSquare.tile.c;
			if (letter != null && getTransitions(node).contains(letter)) {
				final DAWGNode nextNode = node.transition(letter);
				if (!possibleNextSquare.isLastOfLine(ctx.direction)) {
					extendRight(ctx, partialWord + letter, nextNode, this.grid.getNext(possibleNextSquare, ctx.direction));
				}
			}
		}

	}

	private void addLegalMove(final CalculateCtx ctx, final Square endSquare, final String word) {
		Square startSquare = endSquare;
		for (int i = 0; i < word.length() - 1; i++) {
			startSquare = this.grid.getPrevious(startSquare, ctx.direction);
		}
		ctx.legalPlayTiles.add(
				Coordinate.getNotation(startSquare, ctx.direction) + " " + word
		);
	}

	private Set<Character> getAllowedCrossCharacters(final CalculateCtx ctx,
													 final Square crossSquare,
													 final Grid.Direction crossDirection
	) {
		if (!crossSquare.isEmpty()) {
			throw new IllegalStateException("Should not be called on occupied square");
		}

		final Map<Square, Set<Character>> crossChecks = ctx.crosschecks.get(crossDirection);
		if (!crossChecks.containsKey(crossSquare)) {
			final TreeSet<Character> allowed = new TreeSet<>();
			final StringBuilder sb = new StringBuilder();

			Square square = this.grid.getPrevious(crossSquare, crossDirection);
			while (!square.isBorder() && !square.isEmpty()) {
				sb.insert(0, square.tile.c);
				square = this.grid.getPrevious(square, crossDirection);
			}

			final int emptySquare = sb.length();
			sb.append(" ");

			square = this.grid.getNext(crossSquare, crossDirection);
			while (!square.isBorder() && !square.isEmpty()) {
				sb.append(square.tile.c);
				square = this.grid.getNext(square, crossDirection);
			}

			final boolean allowAll = sb.length() == 1;

			for (char letter = 'A'; letter < 'Z'; letter++) {
				sb.setCharAt(emptySquare, letter);
				if (allowAll || this.automaton.contains(sb.toString())) {
					allowed.add(letter);
				}
			}
			crossChecks.put(crossSquare, allowed);
		}
		return crossChecks.get(crossSquare);
	}

	static class CalculateCtx {
		Grid.Direction direction;
		Square anchor;
		Grid grid;

		List<Character> rack;
		Set<String> legalPlayTiles = new LinkedHashSet<>();

		final Map<Grid.Direction, Map<Square, Set<Character>>> crosschecks = new HashMap<>();

		{
			this.crosschecks.put(Grid.Direction.HORIZONTAL, new HashMap<>());
			this.crosschecks.put(Grid.Direction.VERTICAL, new HashMap<>());
		}
	}
}