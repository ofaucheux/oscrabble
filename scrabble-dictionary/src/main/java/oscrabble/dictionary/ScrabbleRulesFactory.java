package oscrabble.dictionary;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import oscrabble.data.ScrabbleRules;

import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;

/**
 * Factory for ScrabbleRules
 */
public class ScrabbleRulesFactory
{
	private static final CSVFormat letterFileFormat = CSVFormat.newFormat(',')
			.withFirstRecordAsHeader();

	public static ScrabbleRules create(final Language language)
	{
		final ScrabbleRules rules = new ScrabbleRules();
		try
		{
			final String namePrefix = language.directoryName + "/";
			rules.letters = new LinkedHashMap<>();
			rules.gridSize = 15;
			try (InputStream is = ScrabbleRulesFactory.class.getResourceAsStream(namePrefix + "tiles.csv"))
			{
				for (final CSVRecord record : new CSVParser(new InputStreamReader(is), letterFileFormat).getRecords())
				{
					final String character = record.get("character");
					if ("blank".equals(character.toLowerCase()))
					{
						rules.numberBlanks = Integer.parseInt(record.get("prevalence"));
					}
					else
					{
						if (character.length() != 1)
						{
							throw new AssertionError("False character: " + character);
						}

						final ScrabbleRules.Letter letter = ScrabbleRules.Letter.builder()
								.c(character.charAt(0))
								.prevalence(Integer.parseInt(record.get("prevalence")))
								.points(Integer.parseInt(record.get("points")))
								.build();
						rules.letters.put(letter.c, letter);
					}
				}
			}
		}
		catch (IOException e)
		{
			throw new IOError(e);
		}

		return rules;
	}
}
