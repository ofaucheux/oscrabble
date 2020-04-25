package oscrabble.server;

import org.apache.commons.io.IOUtils;

import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class MockDictionary implements IDictionary
{

	private static HashSet<String> WORDS;

	static
	{
		try
		{
			WORDS = new HashSet<>(IOUtils.readLines(MockDictionary.class.getResourceAsStream("admissibleWords.json")));
		}
		catch (IOException e)
		{
			throw new IOError(e);
		}
	}

	@Override
	public Collection<String> getAdmissibleWords()
	{
		return WORDS;
	}

	@Override
	public boolean isAdmissible(final String word)
	{
		return WORDS.contains(word);
	}

	public LetterInformation getScrabbleRules()
	{
		return null;// TODO
	}
}
