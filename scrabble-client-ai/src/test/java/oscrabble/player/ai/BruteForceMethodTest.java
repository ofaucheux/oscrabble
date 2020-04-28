package oscrabble.player.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quinto.dawg.DAWGNode;
import oscrabble.*;
import oscrabble.controller.MicroServiceDictionary;
import oscrabble.data.objects.Grid;

import java.net.URI;
import java.text.ParseException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class BruteForceMethodTest
{

	private static final Random RANDOM = new Random();
	private BruteForceMethod instance;

	public static final MicroServiceDictionary DICTIONARY = new MicroServiceDictionary(URI.create("http://localhost:8080"), "FRENCH");
	private TestGrid grid;


	@BeforeEach
	public void BruteForceTest()
	{
		this.instance = new BruteForceMethod(DICTIONARY);
//		this.instance.loadDictionary(DICTIONARY);
		grid = new TestGrid(12);
		this.instance.grid = grid;
	}

	@Test
	void loadDictionary()
	{
		for (final String word : Arrays.asList("HERBE", "AIMEE", "A", "ETUVES"))
		{
			assertTrue(this.instance.automaton.contains(word), "Nicht gefunden: " + word);
		}

		DAWGNode node = this.instance.automaton.getSourceNode();
		for (final char c : "VACANCE".toCharArray())
		{
			node = node.transition(c);
		}
		assertNotNull(node);
		assertTrue(node.isAcceptNode());

		node = this.instance.automaton.getSourceNode();
		for (final char c : "VACANC".toCharArray())
		{
			node = node.transition(c);
		}
		assertNotNull(node);
		assertFalse(node.isAcceptNode());
	}

//
//	@Test
//	void leftPart()
//	{
//		final BruteForceMethod.CalculateCtx ctx = new BruteForceMethod.CalculateCtx();
//		ctx.rack = new Rack();
//		for (final Character c : Arrays.asList('A', 'P', 'U', 'M', 'Q', 'M', 'E', 'X', 'T', 'O'))
//		{
//			ctx.rack.add(Tile.SIMPLE_GENERATOR.generateStone(c));
//		}
//	}

	@Test
	void getLegalMoves() throws ScrabbleException
	{
		final Random random = new Random();
		for (int i = 0; i < 100; i++)
		{
			grid.clear();
//			grid.put(1, 1, Move.Direction.HORIZONTAL, "Z");
			grid.put(1, 1, Grid.Direction.HORIZONTAL, "MANDANT");
			grid.put(4, 1, Grid.Direction.VERTICAL, "DECIME");
			grid.put(1, 4, Grid.Direction.HORIZONTAL, "FINIES");
//			final TextClient textClient = new TextClient(grid);
//			textClient.refreshGrid();


//			final Rack rack = new Rack();
//			for (final char c : "ENFANITS".toCharArray())
//			{
//				rack.add(Tile.SIMPLE_GENERATOR.generateStone(c));
//			}
			final List<String> legalMoves = new ArrayList<>(this.instance.getLegalMoves("ENFANITS"));
			grid.put(legalMoves.get(random.nextInt(legalMoves.size())));
			textClient.refreshGrid();
		}
	}

	@Test
	void testBlank() throws ParseException, ScrabbleException
	{
		final Grid grid = new Grid(16);
		grid.put((PlayTiles) PlayTiles.parseMove(grid, "J2 ELEPHANT"));
		final Rack rack = new Rack();
		for (final char c : "ASMETH".toCharArray())
		{
			rack.add(Tile.SIMPLE_GENERATOR.generateStone(c));
		}
		rack.add(Tile.SIMPLE_GENERATOR.generateStone(null));
		final Set<PlayTiles> playTiles = this.instance.getLegalMoves(grid, rack);
		assertTrue(playTiles.contains(PlayTiles.parseMove(grid, "5J PHASME")));
		assertTrue(playTiles.contains(PlayTiles.parseMove(grid, "5J PhASME")));
	}

	@Test
	void getAnchors() throws ScrabbleException
	{
		final Grid grid = new Grid( 6);
		grid.put(new PlayTiles(grid.getSquare(3, 3), PlayTiles.Direction.HORIZONTAL, "Z"));
		final Set<Grid.Square> anchors = this.instance.getAnchors(grid);
		assertEquals(4, anchors.size());
		assertFalse(anchors.contains(grid.new Square(2, 2)));
		assertTrue(anchors.contains(grid.new Square(2, 3)));
	}

	@Test
	void selectMethod()
	{
		final BruteForceMethod.Player player = instance.new Player("Test client");
		player.editParameters();
		player.editParameters();
	}

	/**
	 * Grid with extended functions.
	 */
	private class TestGrid extends Grid
	{
		public TestGrid(final int gridSize)
		{
			super(gridSize);
		}

		public void put(final int x, final int y, final Direction direction, final String word)
		{
			Square sq = this.get(x - 1, y - 1);
			for (int i = 0; i < word.length(); i++)
			{
				sq.c = word.charAt(i);
				sq = sq.getNeighbour(direction, 1);
			}
		}

		public void clear()
		{
			final int size = this.getSize();
			for (int i = 0; i < (size ^ 2); i++)
			{
				get(i % size, i / size).c = null;
			}
		}
	}
}