package oscrabble.data;

import lombok.Data;

import java.util.Collection;
import java.util.Map;

/**
 * A dictionary
 */
public interface IDictionary
{
	/**
	 * @return all admissible words
	 */
	Collection<String> getAdmissibleWords();

	/**
	 * @param word word
	 * @return if the word is admissible
	 */
	boolean isAdmissible(String word);

	/**
	 * @return meta infos
	 */
	ScrabbleRules getScrabbleRules();

	/**
	 * Information about a letter
	 */
	@Data
	class LetterMetaInfo
	{
		char c;
		public int prevalence;
		public int points;
	}


	/**
	 * Information about the distribution of letters.
	 */
	@Data
	class ScrabbleRules
	{
		/**
		 * Blancs are represented with the space character.
		 */
		public Map<Character, LetterMetaInfo> letters;
		public int gridSize;

		/**
		 * This limit is 7 for French and German Scrabble, could be another one of other languages. see https://www.fisf.net/scrabble/decouverte-du-scrabble/formules-de-jeu.html and Turnierspielordnung of Scrabble Deutschland e.V.
		 */
		public int requiredTilesInBagForExchange = 7;
	}

}
