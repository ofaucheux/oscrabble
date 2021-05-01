package oscrabble.player.ai;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Triple;
import oscrabble.data.DictionaryEntry;
import oscrabble.data.IDictionary;
import oscrabble.data.ScrabbleRules;
import oscrabble.dictionary.DictionaryException;

import java.io.IOError;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

/**
 * A french dictionary with only a part of the world list
 */
public class FrenchDictionaryForTest implements IDictionary {
	private final Set<String> words = new HashSet<>();

	FrenchDictionaryForTest() throws IOError {
		try {
			words.addAll(IOUtils.readLines(FrenchDictionaryForTest.class.getResourceAsStream("PartialFrenchWordList"), Charset.defaultCharset()));
		} catch (IOException e) {
			throw new IOError(e);
		}
	}

	@Override
	public Collection<String> getAdmissibleWords() {
		return this.words;
	}

	@Override
	public boolean isAdmissible(final String word) {
		return this.words.contains(word);
	}

	@Override
	public ScrabbleRules getScrabbleRules() {
		final ArrayList<Triple<Character, Integer, Integer>> letters = new ArrayList<>();
		letters.add(Triple.of('E', 15, 1));
		letters.add(Triple.of('A', 9, 1));
		letters.add(Triple.of('I', 8, 1));
		letters.add(Triple.of('N', 6, 1));
		letters.add(Triple.of('O', 6, 1));
		letters.add(Triple.of('R', 6, 1));
		letters.add(Triple.of('S', 6, 1));
		letters.add(Triple.of('T', 6, 1));
		letters.add(Triple.of('U', 6, 1));
		letters.add(Triple.of('L', 5, 1));
		letters.add(Triple.of('D', 3, 2));
		letters.add(Triple.of('M', 3, 2));
		letters.add(Triple.of('G', 2, 2));
		letters.add(Triple.of('B', 2, 3));
		letters.add(Triple.of('C', 2, 3));
		letters.add(Triple.of('P', 2, 3));
		letters.add(Triple.of('F', 2, 4));
		letters.add(Triple.of('H', 2, 4));
		letters.add(Triple.of('V', 2, 4));
		letters.add(Triple.of('J', 1, 8));
		letters.add(Triple.of('Q', 1, 8));
		letters.add(Triple.of('K', 1, 10));
		letters.add(Triple.of('W', 1, 10));
		letters.add(Triple.of('X', 1, 10));
		letters.add(Triple.of('Y', 1, 10));
		letters.add(Triple.of('Z', 1, 10));

		final ScrabbleRules rules = new ScrabbleRules();
		rules.gridSize = 7;
		rules.numberBlanks = 2;
		rules.letters = new HashMap<>();
		for (final Triple<Character, Integer, Integer> triple : letters) {
			rules.letters.put(
					triple.getLeft(),
					ScrabbleRules.Letter.builder()
							.c(triple.getLeft())
							.prevalence(triple.getMiddle())
							.points(triple.getRight())
							.build()
			);
		}

		return rules;
	}

	@Override
	public DictionaryEntry getEntry(final String word) throws DictionaryException {
		return DictionaryEntry.builder().definitions(Arrays.asList("No def in the test dictionary")).build();
	}
}
