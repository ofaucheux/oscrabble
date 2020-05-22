package oscrabble.player.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.quinto.dawg.DAWGNode;
import oscrabble.ScrabbleException;
import oscrabble.controller.MicroServiceDictionary;
import oscrabble.controller.MicroServiceScrabbleServer;
import oscrabble.data.objects.Grid;
import oscrabble.player.AbstractPlayer;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

@Disabled // todo: change
public class BruteForceMethodTest
{

	private BruteForceMethod instance;

	private final static MicroServiceDictionary DICTIONARY =  MicroServiceDictionary.getDefaultFrench();
	private final static MicroServiceScrabbleServer server = new MicroServiceScrabbleServer("localhost", MicroServiceScrabbleServer.DEFAULT_PORT);

	private ArrayBlockingQueue<String> playQueue;
	private AbstractPlayer player;


	@BeforeEach
	public void BruteForceTest()
	{
		this.instance = new BruteForceMethod(DICTIONARY);
	}

	@Test
	void loadDictionary()
	{
		for (final String word : Arrays.asList("HERBE", "AIMEE", "ETUVES"))
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
		final UUID game = server.newGame();
		this.instance.grid = server.getGrid(game);
		final List<String> legalMoves = new ArrayList<>(getLegalMoves(this.instance, "ENFANIT"));

		assertTrue(legalMoves.contains("F8 ENFANT"));

		// todo: reactivate after possibility of rollback
		// Test a lot of found possibilities
//		for (int i = 0; i < 100; i++)
//		{
//			this.playQueue.add(legalMoves.get(random.nextInt(legalMoves.size())));
//			server.awaitEndOfPlay(game, 1);
////			assertFalse(this.player.isLastPlayError);
////			server.rollbackLastMove(this.player);
//		}
	}

	public Set<String> getLegalMoves(final BruteForceMethod bfm, final String rack)
	{
		final ArrayList<Character> list = new ArrayList<>(rack.length());
		rack.chars().forEach(c -> list.add((char) c));
		return bfm.getLegalMoves(list);
	}

	@Test
	void testBlank() throws  ScrabbleException
	{
		startGame("ELEPHANT");

		this.instance.grid = new Grid();
		this.instance.grid.play(null, "2J ELEPHANT");
		final Set<String> playTiles = getLegalMoves(this.instance, "ASME TH");
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
	@Disabled //todo
	private void startGame(final String bag) throws ScrabbleException
	{
		//TODO
//		server.assertFirstLetters(bag);
//		this.player = new AIPlayer(this.instance, "AI Player");
//		server.addPlayer(this.player);
//
//		this.playQueue = new ArrayBlockingQueue<>(100);
//		server.addListener(new AbstractGameListener()
//		{
//			@Override
//			public void onPlayRequired(final UUID player)
//			{
//				try
//				{
//					BruteForceMethodTest.this.instance.grid = BruteForceMethodTest.server.getGrid();
//					BruteForceMethodTest.server.play(player, Action.parse(BruteForceMethodTest.this.playQueue.take()));
//				}
//				catch (ScrabbleException.ForbiddenPlayException | InterruptedException | ScrabbleException.NotInTurn e)
//				{
//					throw new Error(e);
//				}
//			}
//		});
//		new Thread(() -> server.play()).start();
	}

}