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
	LetterInformation getLetterMetaInfo();

	/**
	 * Information about a letter
	 */
	@Data
	class LetterMetaInfo
	{
		char c;
		int prevalence;
		int point;
	}


	/**
	 * Information about the distribution of letters.
	 */
	@Data
	class LetterInformation
	{
		Map<Character, LetterMetaInfo> letters;
		int numberBlanks;
	}

}
