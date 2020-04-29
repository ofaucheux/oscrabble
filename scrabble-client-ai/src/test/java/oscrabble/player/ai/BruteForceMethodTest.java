package oscrabble.player.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quinto.dawg.DAWGNode;
import oscrabble.*;
import oscrabble.controller.MicroServiceDictionary;
import oscrabble.data.objects.Grid;
import oscrabble.server.AbstractGameListener;
import oscrabble.server.Game;
import oscrabble.controller.Action;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class BruteForceMethodTest
{

	private static final Random RANDOM = new Random();
	private BruteForceMethod instance;

	public static final MicroServiceDictionary DICTIONARY = new MicroServiceDictionary(URI.create("http://localhost:8080/"), "FRENCH");
	private TestGrid grid;
	private ArrayBlockingQueue<String> playQueue;
	private Game game;
	private Game.Player player;


	@BeforeEach
	public void BruteForceTest() throws ScrabbleException
	{
		this.instance = new BruteForceMethod(DICTIONARY);
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
	void getLegalMoves() throws ScrabbleException, InterruptedException, TimeoutException
	{
		startGame("ENFANIT");

		final Random random = new Random();
		this.instance.grid = this.game.getGrid();
		final List<String> legalMoves = new ArrayList<>(this.instance.getLegalMoves("ENFANIT"));

		assertTrue(legalMoves.contains("F8 ENFANT"));

		// Test a lot of found possibilities
		for (int i = 0; i < 100; i++)
		{
			this.playQueue.add(legalMoves.get(random.nextInt(legalMoves.size())));
			this.game.awaitEndOfPlay(1);
			assertFalse(this.player.isLastPlayError);
			this.game.rollbackLastMove(this.player);
		}
	}

	@Test
	void testBlank() throws  ScrabbleException
	{
		startGame("ELEPHANT");

		this.instance.grid = new Grid();
		this.instance.grid.play("2J ELEPHANT");
		final Set<String> playTiles = this.instance.getLegalMoves("ASME TH");
		assertTrue(playTiles.contains("J5 PHASME"));
		assertTrue(playTiles.contains("J5 PhASME"));
	}

//	@Test
//	void getAnchors() throws ScrabbleException
//	{
//		final Grid grid = new Grid( 6);
//		grid.put(new PlayTiles(grid.getSquare(3, 3), PlayTiles.Direction.HORIZONTAL, "Z"));
//		final Set<Grid.Square> anchors = this.instance.getAnchors(grid);
//		assertEquals(4, anchors.size());
//		assertFalse(anchors.contains(grid.new Square(2, 2)));
//		assertTrue(anchors.contains(grid.new Square(2, 3)));
//	}
//
//	@Test
//	void selectMethod()
//	{
//		final BruteForceMethod.Player player = instance.new Player("Test client");
//		player.editParameters();
//		player.editParameters();
//	}

	/**
	 *
	 * @param bag first letters the bag must deliver.
	 */
	private void startGame(final String bag) throws ScrabbleException
	{
		this.game = new Game(DICTIONARY);
		this.game.assertFirstLetters(bag);
		this.player = new Game.Player("AI Player");
		this.game.addPlayer(this.player);
		this.playQueue = new ArrayBlockingQueue<>(100);
		this.game.addListener(new AbstractGameListener()
		{
			@Override
			public void onPlayRequired(final Game.Player player)
			{
				try
				{
					BruteForceMethodTest.this.instance.grid = BruteForceMethodTest.this.game.getGrid();
					BruteForceMethodTest.this.game.play(player, Action.parse(BruteForceMethodTest.this.playQueue.take()));
				}
				catch (ScrabbleException.ForbiddenPlayException | InterruptedException | ScrabbleException.NotInTurn e)
				{
					throw new Error(e);
				}
			}
		});
		new Thread(() -> this.game.play()).start();
	}

	/**
	 * Grid with extended functions.
	 */
	private class TestGrid extends Grid
	{
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