package oscrabble.dictionary;


import org.junit.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DictionaryTest
{

	@Test
	public void toUpperCase()
	{
		final Dictionary german = Dictionary.getDictionary(Language.GERMAN);
		assertEquals("ÄCHSEND", german.toUpperCase("ächsend"));
		assertEquals("CAFE", german.toUpperCase("Café"));

		final Dictionary french = Dictionary.getDictionary(Language.FRENCH);
		assertEquals("AIGUE", french.toUpperCase("aigüe"));
	}


	private final Dictionary french = Dictionary.getDictionary(Language.FRENCH);

	@Test
	public void getWords()
	{
		assertTrue(this.french.containUpperCaseWord("CA"));
		assertTrue(this.french.getMutations().contains("ETERNUER"));
		assertFalse(this.french.containUpperCaseWord("MEN"));
	}

	@Test
	public void containUpperCaseWord()
	{
		Dictionary.getDictionary(Language.FRENCH);
		assertTrue(this.french.containUpperCaseWord("PIECE"));
		assertTrue(this.french.containUpperCaseWord("LIVREE"));
		assertTrue(this.french.containUpperCaseWord("MIMEES"));
		assertFalse(this.french.containUpperCaseWord("livrée"));
		assertTrue(this.french.containUpperCaseWord("YETI"));
	}

	public static Dictionary getTestDictionary()
	{
		return Dictionary.getDictionary(Language.TEST);
	}
}