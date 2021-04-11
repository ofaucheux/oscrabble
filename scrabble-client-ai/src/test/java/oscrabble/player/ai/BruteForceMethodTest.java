package oscrabble.player.ai;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.io.IOUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.quinto.dawg.DAWGNode;
import oscrabble.ScrabbleException;
import oscrabble.controller.Action;
import oscrabble.data.IDictionary;
import oscrabble.data.objects.Grid;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class BruteForceMethodTest {

	private BruteForceMethod instance;

	private final static IDictionary DICTIONARY = new FrenchDictionaryForTest();

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

		final TreeSet<String> moves = new TreeSet<>(getLegalMoves(this.instance, testParams.rack));
		MatcherAssert.assertThat(grid.toString(), moves, CoreMatchers.hasItems(testParams.movesToFind.toArray(new String[0])));

		final TreeSet<String> foundWords = new TreeSet<>();
		moves.forEach(m -> foundWords.add(Action.parsePlayNotation(m).getRight()));
		MatcherAssert.assertThat(DICTIONARY.getAdmissibleWords(), CoreMatchers.hasItems(foundWords.toArray(new String[0])));
	}

	public List<String> getLegalMoves(final BruteForceMethod bfm, final String rack) {
		final ArrayList<Character> list = new ArrayList<>(rack.length());
		rack.chars().forEach(c -> list.add((char) c));
		return bfm.getLegalMoves(list, new Strategy.BestSize()).getLeft();
	}

	@Test
	void testBlank() throws ScrabbleException {
		this.instance.setGrid(new Grid());
		this.instance.grid.play(null, "J2 ELEPHANT");
		final List<String> playTiles = getLegalMoves(this.instance, "ASME TH");
		assertTrue(playTiles.contains("5J PHASME"));
		assertTrue(playTiles.contains("5J PhASME"));
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