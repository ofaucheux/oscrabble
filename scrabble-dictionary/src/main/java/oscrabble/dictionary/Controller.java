package oscrabble.dictionary;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import oscrabble.data.ScrabbleRules;

import java.util.Collection;

@SuppressWarnings("unused")
@org.springframework.stereotype.Controller
public class Controller
{

	/**
	 * Testet if a word is accepted as scrabble word.
	 *
	 * @param language Language
	 * @param word     word to test, with accents
	 * @return 200 with list of variants if the word is accepted, 404 else.
	 */
	@GetMapping(value = "/{language}/word/{word}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Collection<Dictionary.Mutation>> getWord(
			final @PathVariable("language") String language,
			final @PathVariable("word") String word
	) throws UnknownLanguage, DictionaryException
	{
		final Dictionary d = Dictionary.getDictionary(getLanguage(language));
		final String uc = d.toUpperCase(word);
		final boolean accepted = d.containUpperCaseWord(uc);
		if (accepted)
		{
			return new ResponseEntity<>(d.getMutations(uc), HttpStatus.OK);
		}
		else
		{
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
	}

	/**
	 * @param language language
	 * @return list of the admissible words (all uppercase)
	 * @throws UnknownLanguage
	 */
	@GetMapping("/{language}/getAdmissibleWords")
	public ResponseEntity<Collection<String>> getAdmissibleWords(
			@PathVariable("language") final String language
	) throws UnknownLanguage
	{
		return new ResponseEntity<>(Dictionary.getDictionary(getLanguage(language)).words.keySet(), HttpStatus.OK);
	}

	/**
	 * @return list of the letters and their properties
	 * @throws UnknownLanguage
	 */
	@GetMapping("/{language}/getScrabbleRules")
	public ResponseEntity<ScrabbleRules> getScrabbleRules(
			@PathVariable("language") final String language
	) throws UnknownLanguage
	{
		return new ResponseEntity<>(ScrabbleRulesFactory.create(getLanguage(language)), HttpStatus.OK);
	}

	/**
	 *
	 * @param language name of the language
	 * @return the language
	 * @throws UnknownLanguage if no such language
	 */
	private static Language getLanguage(@PathVariable("language") final String language) throws UnknownLanguage
	{
		final Language l;
		try
		{
			l = Language.valueOf(language);
		}
		catch (IllegalArgumentException e)
		{
			throw new UnknownLanguage(language);
		}
		return l;
	}


	/**
	 * Error indicating an unknown language.
	 */
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public static class UnknownLanguage extends Exception
	{
		public UnknownLanguage(final String language)
		{
			super("Unknown language: " + language);
		}
	}
}