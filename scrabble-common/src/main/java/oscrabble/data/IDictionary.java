package oscrabble.data;

import java.util.Collection;

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
	oscrabble.data.ScrabbleRules getScrabbleRules();

}