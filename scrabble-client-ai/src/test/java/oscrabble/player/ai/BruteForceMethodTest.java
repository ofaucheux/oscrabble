package oscrabble.player.ai;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.io.IOUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.quinto.dawg.DAWGNode;
import oscrabble.ScrabbleException;
import oscrabble.controller.Action;
import oscrabble.controller.MicroServiceScrabbleServer;
import oscrabble.data.IDictionary;
import oscrabble.data.objects.Grid;
import oscrabble.player.AbstractPlayer;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TODO: reactivate
 */
@Disabled
public class BruteForceMethodTest {

	private BruteForceMethod instance;

	private final static IDictionary DICTIONARY = new FrenchDictionaryForTest();
	private final static MicroServiceScrabbleServer server = MicroServiceScrabbleServer.getLocal();

	private ArrayBlockingQueue<String> playQueue;
	private AbstractPlayer player;

	private static List<TestData> findMoveParameterProvider() {
		return List.of(
				new TestData("grid_1.grid", "EDPWMES", Set.of("15J"), Set.of("15H MES")),
				new TestData("grid_2.grid", "EDPWMES", Set.of("14J"), Set.of("14H MES"))
		);
	}

	@BeforeEach
	public void BruteForceTest() {
		this.instance = new BruteForceMethod(DICTIONARY);
	}

	@Test
	void loadDictionary() {
		for (final String word : Arrays.asList("HERBE", "AIMEE", "ETUVES")) {
			assertTrue(this.instance.automaton.contains(word), "Nicht gefunden: " + word);
		}

		DAWGNode node = this.instance.automaton.getSourceNode();
		for (final char c : "VACANCE".toCharArray()) {
			node = node.transition(c);
		}
		assertNotNull(node);
		assertTrue(node.isAcceptNode());

		node = this.instance.automaton.getSourceNode();
		for (final char c : "VACANC".toCharArray()) {
			node = node.transition(c);
		}
		assertNotNull(node);
		assertFalse(node.isAcceptNode());
	}

	@Test
	void getLegalMoves() throws ScrabbleException {
		startGame("ENFANIT");

		final UUID game = server.newGame();
		this.instance.setGrid(server.getGrid(game));
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

	/**
	 * @param testParams
	 * @throws IOException
	 */
	@ParameterizedTest
	@MethodSource("findMoveParameterProvider")
	void getLegalMoves(final TestData testParams) throws IOException {
		final String asciiArt = IOUtils.toString(
				BruteForceMethodTest.class.getResourceAsStream(testParams.filename),
				Charset.defaultCharset()
		);
		final Grid grid = Grid.fromAsciiArt(DICTIONARY.getScrabbleRules(), asciiArt);
		this.instance.setGrid(grid);

		Collection<String> missing;
		final Set<String> anchors = new TreeSet<>();
		this.instance.getAnchors()
				.forEach(a -> anchors.add(a.getCoordinate()));
		missing = CollectionUtils.subtract(
				testParams.anchorsToFind,
				anchors
		);
		MatcherAssert.assertThat(
				grid.toString(),
				anchors,
				CoreMatchers.hasItems(testParams.anchorsToFind.toArray(new String[0]))
		);
		assertTrue(missing.isEmpty(), "Missing anchors: " + missing);

		final Set<String> moves = new TreeSet(getLegalMoves(this.instance, testParams.rack));
		MatcherAssert.assertThat(grid.toString(), moves, CoreMatchers.hasItems(testParams.movesToFind.toArray(new String[0])));

		final TreeSet<String> foundWords = new TreeSet<>();
		moves.forEach(m -> {
			foundWords.add(Action.parsePlayNotation(m).getRight());
		});
		MatcherAssert.assertThat(DICTIONARY.getAdmissibleWords(), CoreMatchers.hasItems(foundWords.toArray(new String[0])));
	}

	public List<String> getLegalMoves(final BruteForceMethod bfm, final String rack) {
		final ArrayList<Character> list = new ArrayList<>(rack.length());
		rack.chars().forEach(c -> list.add((char) c));
		final ListIterator<String> iterator = bfm.getLegalMoves(list, new Strategy.BestSize());
		// rewind
		while (iterator.hasPrevious()) {
			iterator.previous();
		}
		return IteratorUtils.toList(iterator);
	}

	@Test
	void testBlank() throws ScrabbleException {
		startGame("ELEPHANT");

		this.instance.setGrid(new Grid());
		this.instance.grid.play(null, "2J ELEPHANT");
		final List<String> playTiles = getLegalMoves(this.instance, "ASME TH");
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
	 * @param bag first letters the bag must deliver.
	 */
	@Disabled //todo
	private void startGame(final String bag) {
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

	/**
	 * Data for a test.
	 */
	private static class TestData {
		private final String filename;
		private final String rack;
		private final Set<String> movesToFind;
		private final Set<String> anchorsToFind;

		public TestData(final String filename, final String rack, final Set<String> anchorsToFind, final Set<String> movesToFind) {
			this.filename = filename;
			this.rack = rack;
			this.anchorsToFind = anchorsToFind;
			this.movesToFind = movesToFind;
		}
	}
}