package oscrabble.player;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quinto.dawg.DAWGNode;
import oscrabble.*;
import oscrabble.action.PlayTiles;
import oscrabble.dictionary.Dictionary;
import oscrabble.client.TextClient;

import java.text.ParseException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class BruteForceMethodTest
{

	private static final Random RANDOM = new Random();
	private BruteForceMethod instance;

	@BeforeEach
	public void BruteForceTest()
	{
		final Dictionary french = Dictionary.getDictionary(Dictionary.Language.FRENCH);
		this.instance = new BruteForceMethod(french);
		this.instance.loadDictionary(french);
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


	@Test
	void leftPart()
	{
		final BruteForceMethod.CalculateCtx ctx = new BruteForceMethod.CalculateCtx();
		ctx.rack = new Rack();
		for (final Character c : Arrays.asList('A', 'P', 'U', 'M', 'Q', 'M', 'E', 'X', 'T', 'O'))
		{
			ctx.rack.add(Tile.SIMPLE_GENERATOR.generateStone(c));
		}
	}

	@Test
	void getLegalMoves() throws ScrabbleException
	{
		for (int i = 0; i < 100; i++)
		{
			final Grid grid = new Grid(12);
//			grid.put(1, 1, Move.Direction.HORIZONTAL, "Z");
			grid.put(new PlayTiles(grid.getSquare(1, 1), PlayTiles.Direction.HORIZONTAL, "MANDANT"));
			grid.put(new PlayTiles(grid.getSquare(4, 1), PlayTiles.Direction.VERTICAL, "DECIME"));
			grid.put(new PlayTiles((grid.getSquare(1, 4)), PlayTiles.Direction.HORIZONTAL, "FINIES"));
			final TextClient textClient = new TextClient(grid);
			textClient.refreshGrid();


			final Rack rack = new Rack();
			for (final char c : "ENFANITS".toCharArray())
			{
				rack.add(Tile.SIMPLE_GENERATOR.generateStone(c));
			}
			final List<PlayTiles> legalPlayTiles = new ArrayList<>(this.instance.getLegalMoves(grid, rack));
			final PlayTiles playTiles = legalPlayTiles.get(RANDOM.nextInt(legalPlayTiles.size()));
			grid.put(playTiles);
			textClient.refreshGrid();
		}
	}

	@Test
	void testBlank() throws ParseException, ScrabbleException
	{
		final Grid grid = new Grid(16);
		grid.put(PlayTiles.parseMove(grid, "J2 ELEPHANT"));
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
}