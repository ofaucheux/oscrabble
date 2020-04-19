package oscrabble.dictionary;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Collection;
import java.util.HashMap;

@org.springframework.stereotype.Controller
public class Controller
{

	public static final HashMap<Language, ScrabbleLanguageInformation> SLI_MAP = new HashMap<>();
	public static final HashMap<Language, Dictionary> DICTIONARY_MAP = new HashMap<>();

	@GetMapping("/isAdmissibleInScrabble")
	public ResponseEntity<Collection<String>> isAdmissible(final String language, final String scrabbleWord)
	{
		Language l;
		try
		{
			l = Language.valueOf(language);
		}
		catch (IllegalArgumentException e)
		{
			throw new IllegalArgumentException("Language " + language + " unknown");
		}

		final Dictionary d = Dictionary.getDictionary(l);
		final boolean accepted = d.containUpperCaseWord(scrabbleWord);
		if (accepted)
		{
			return new ResponseEntity<>(d.getMutations(scrabbleWord), HttpStatus.OK);
		}
		else
		{
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
	}
}
