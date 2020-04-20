package oscrabble.dictionary;

import lombok.Data;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Information relative to the use of a language in scrabble: number of tiles, etc.
 */
@Data
public class ScrabbleLetterInformation
{
	private static final CSVFormat letterFileFormat = CSVFormat.newFormat(',')
			.withFirstRecordAsHeader();

	public final Map<Character, Letter> letters;

	public int numberBlanks;

	public ScrabbleLetterInformation(final Language language)
	{
		try
		{
			final String namePrefix = language.directoryName + "/";
			this.letters = new LinkedHashMap<>();
			try (InputStream is = ScrabbleLetterInformation.class.getResourceAsStream(namePrefix + "tiles.csv"))
			{
				for (final CSVRecord record : new CSVParser(new InputStreamReader(is), letterFileFormat).getRecords())
				{
					final Letter letter = new Letter();
					final String character = record.get("character");
					if ("blank".equals(character.toLowerCase()))
					{
						this.numberBlanks = Integer.parseInt(record.get("prevalence"));
					}
					else
					{
						if (character.length() != 1)
						{
							throw new AssertionError("False character: " + character);
						}
						letter.c = character.charAt(0);
						letter.prevalence = Integer.parseInt(record.get("prevalence"));
						letter.points = Integer.parseInt(record.get("points"));
						this.letters.put(letter.c, letter);
					}
				}
			}
		}
		catch (IOException e)
		{
			throw new IOError(e);
		}

	}

	/**
	 * Information about the prevalence of a letter.
	 */
	@Data
	public static class Letter
	{
		public char c;
		public int prevalence;
		int points;
	}

}
