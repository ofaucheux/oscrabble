package oscrabble.dictionary;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DictionaryTest
{

	@Test
	void toUpperCase()
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
		assertTrue(this.french.containUpperCaseWord("LE"));
		assertTrue(this.french.getAdmissibleWords().contains("ETERNUER"));
		assertFalse(this.french.containUpperCaseWord("MEN"));
	}

	@Test
	public void containUpperCaseWord()
	{
		Dictionary.getDictionary(Language.FRENCH);
		assertTrue(this.french.containUpperCaseWord("PIECE"));
		assertTrue(this.french.containUpperCaseWord("LIVREE"));
		assertTrue(this.french.containUpperCaseWord("MIMEES"));
		assertTrue(this.french.containUpperCaseWord("MIX"));
		assertTrue(this.french.containUpperCaseWord("WHIP"));
		assertTrue(this.french.containUpperCaseWord("WHIPS"));
		assertTrue(this.french.containUpperCaseWord("RIF"));

		assertFalse(this.french.containUpperCaseWord("livrée"));
	}

	public static Dictionary getTestDictionary()
	{
		return Dictionary.getDictionary(Language.TEST);
	}
}