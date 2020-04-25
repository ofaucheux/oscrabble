package oscrabble.data;

import lombok.Data;

import java.util.Map;

/**
 * Information relative to the use of a language in scrabble: number of tiles, etc.
 */
@Data
public class ScrabbleRules
{
	public Map<Character, Letter> letters;

	public int numberBlanks;

	/**
	 * Information about the prevalence of a letter.
	 */
	@Data
	public static class Letter
	{
		public char c;
		public int prevalence;
		public int points;
	}

}

