package oscrabble.data;

import lombok.Data;
import lombok.Builder;

import java.util.Map;

/**
 * Information relative to the use of a language in scrabble: number of tiles, etc.
 */
@Data
public class ScrabbleRules {
	public Map<Character, Letter> letters;

	public int numberBlanks;

	public int gridSize;

	/**
	 * This limit is 7 for French and German Scrabble, could be another one of other languages. see https://www.fisf.net/scrabble/decouverte-du-scrabble/formules-de-jeu.html and Turnierspielordnung of Scrabble Deutschland e.V.
	 */
	public int requiredTilesInBagForExchange = 7;

	/**
	 * @param character
	 * @return number of points for this character
	 */
	public int getPoints(final char character) {
		final Letter letter = this.letters.get(character);
		if (letter == null) {
			throw new IllegalArgumentException("No such letter in the scrabble: " + character);
		}
		return letter.getPoints();
	}

	/**
	 * Information about the prevalence of a letter.
	 */
	@Data
	@Builder
	public static class Letter {
		public char c;
		public int prevalence;
		public int points;
	}

}

