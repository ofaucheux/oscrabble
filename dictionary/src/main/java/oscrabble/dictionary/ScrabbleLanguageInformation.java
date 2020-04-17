package oscrabble.dictionary;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Information relative to the use of a language in scrabble: number of tiles, etc.
 */
public class ScrabbleLanguageInformation implements Tile.Generator
{
	private static final CSVFormat letterFileFormat = CSVFormat.newFormat(',')
			.withFirstRecordAsHeader();

	private final Map<Character, Letter> letters;

	private int numberBlanks;


	public ScrabbleLanguageInformation(final Language language)
	{
		try
		{
			final String namePrefix = language.directoryName + "/";
			this.letters = new LinkedHashMap<>();
			try (InputStream is = ScrabbleLanguageInformation.class.getResourceAsStream(namePrefix + "tiles.csv"))
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

	@Override
	public Tile generateStone(final Character c)
	{
		final Tile tile;
		if (c == null)
		{
			tile = new Tile(null, 0);
		}
		else
		{
			if (!Character.isUpperCase(c))
			{
				throw new AssertionError("Character must be uppercase: " + c);
			}

			final Letter letter = this.letters.get(c);
			tile = new Tile(letter.c, letter.points);
		}
		return tile;
	}

	public Collection<Letter> getLetters()
	{
		return this.letters.values();
	}

	public int getNumberBlanks()
	{
		return this.numberBlanks;
	}

	/**
	 * Information about the prevalence of a letter.
	 */
	public static class Letter
	{
		public char c;
		public int prevalence;
		int points;
	}

}
