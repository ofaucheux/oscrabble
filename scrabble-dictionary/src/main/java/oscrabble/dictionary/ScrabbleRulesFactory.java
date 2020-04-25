package oscrabble.dictionary;

import lombok.Data;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import oscrabble.data.ScrabbleRules;

import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.Map;

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
			try (InputStream is = ScrabbleRulesFactory.class.getResourceAsStream(namePrefix + "tiles.csv"))
			{
				for (final CSVRecord record : new CSVParser(new InputStreamReader(is), letterFileFormat).getRecords())
				{
					final ScrabbleRules.Letter letter = new ScrabbleRules.Letter();
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

						letter.c = character.charAt(0);
						letter.prevalence = Integer.parseInt(record.get("prevalence"));
						letter.points = Integer.parseInt(record.get("points"));
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
