package oscrabble.dictionary;

import org.apache.log4j.BasicConfigurator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DictionaryTest
{
	@BeforeAll
	static void log4j()
	{
		BasicConfigurator.configure();
	}


	@Test
	void toUpperCase()
	{
		final Dictionary german = Dictionary.getDictionary(Dictionary.Language.GERMAN);
		assertEquals("ÄCHSEND", german.toUpperCase("ächsend"));
		assertEquals("CAFE", german.toUpperCase("Café"));

		final Dictionary french = Dictionary.getDictionary(Dictionary.Language.FRENCH);
		assertEquals("AIGUE", french.toUpperCase("aigüe"));
	}


	private final Dictionary french = Dictionary.getDictionary(Dictionary.Language.FRENCH);

	@Test
	void getWords()
	{
		assertTrue(french.getMutations().contains("ETERNUER"));
	}


	@Test
	void getLetters()
	{
		french.getLetters();
	}

	@Test
	void containUpperCaseWord()
	{
		Dictionary.getDictionary(Dictionary.Language.FRENCH);
		assertTrue(french.containUpperCaseWord("PIECE"));
		assertTrue(french.containUpperCaseWord("LIVREE"));
		assertTrue(french.containUpperCaseWord("MIMEES"));
		assertFalse(french.containUpperCaseWord("livrée"));
	}
}