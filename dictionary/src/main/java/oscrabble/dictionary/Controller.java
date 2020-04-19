package oscrabble.dictionary;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

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
	@GetMapping("/isAdmissibleInScrabble/{language}/{word}")
	public ResponseEntity<Collection<String>> isAdmissible(
			final @PathVariable("language") String language,
			final @PathVariable("word") String word
	)
	{
		final Language l;
		try
		{
			l = Language.valueOf(language);
		}
		catch (IllegalArgumentException e)
		{
			throw new IllegalArgumentException("Language " + language + " unknown");
		}

		final Dictionary d = Dictionary.getDictionary(l);
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

	@GetMapping("/getAllAdmissibleWords/{language}")
	public ResponseEntity<Collection<Dictionary.UpperCaseWord>> getAllAdmissibleWords(
			@PathVariable("language") final String language
	)
	{
		return new ResponseEntity<>(Dictionary.getDictionary(Language.valueOf(language)).words.values(), HttpStatus.OK);
	}

}
