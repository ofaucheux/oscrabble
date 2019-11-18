package oscrabble.player;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quinto.dawg.DAWGNode;
import oscrabble.*;
import oscrabble.dictionary.Dictionary;
import oscrabble.client.TextClient;
import oscrabble.server.Game;
import oscrabble.server.GameTest;

import java.text.ParseException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class BruteForceMethodTest
{

	private static final Random RANDOM = new Random();
	public static final Game TEST_SERVER = GameTest.getTestServer();
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
			ctx.rack.add(Stone.SIMPLE_GENERATOR.generateStone(c));
		}
	}

	@Test
	void getLegalMoves() throws ScrabbleException
	{
		for (int i = 0; i < 100; i++)
		{
			final Grid grid = new Grid(12);
//			grid.put(1, 1, Move.Direction.HORIZONTAL, "Z");
			grid.put(new Move(grid.getSquare(1, 1), Move.Direction.HORIZONTAL, "MANDANT"));
			grid.put(new Move(grid.getSquare(4, 1), Move.Direction.VERTICAL, "DECIME"));
			grid.put(new Move((grid.getSquare(1, 4)), Move.Direction.HORIZONTAL, "FINIES"));
			final TextClient textClient = new TextClient(grid);
			textClient.refreshGrid();


			final Rack rack = new Rack();
			for (final char c : "ENFANITS".toCharArray())
			{
				rack.add(Stone.SIMPLE_GENERATOR.generateStone(c));
			}
			final List<Move> legalMoves = new ArrayList<>(this.instance.getLegalMoves(grid, rack));
			final Move move = legalMoves.get(RANDOM.nextInt(legalMoves.size()));
			grid.put(move);
			textClient.refreshGrid();
		}
	}

	@Test
	void testBlank() throws ParseException, ScrabbleException
	{
		final Grid grid = new Grid(16);
		grid.put(Move.parseMove(grid, "J2 ELEPHANT", false));
		final Rack rack = new Rack();
		for (final char c : "ASMETH".toCharArray())
		{
			rack.add(Stone.SIMPLE_GENERATOR.generateStone(c));
		}
		rack.add(Stone.SIMPLE_GENERATOR.generateStone(null));
		final Set<Move> moves = this.instance.getLegalMoves(grid, rack);
		assertTrue(moves.contains(Move.parseMove(grid, "5J PHASME", false)));
		assertTrue(moves.contains(Move.parseMove(grid, "5J PhASME", false)));
	}

	@Test
	void getAnchors() throws ScrabbleException
	{
		final Grid grid = new Grid( 6);
		grid.put(new Move(grid.getSquare(3, 3), Move.Direction.HORIZONTAL, "Z"));
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