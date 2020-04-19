package oscrabble.dictionary;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Collection;
import java.util.HashMap;

@org.springframework.stereotype.Controller
public class Controller
{

	public static final HashMap<Language, ScrabbleLanguageInformation> SLI_MAP = new HashMap<>();
	public static final HashMap<Language, Dictionary> DICTIONARY_MAP = new HashMap<>();

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
}
