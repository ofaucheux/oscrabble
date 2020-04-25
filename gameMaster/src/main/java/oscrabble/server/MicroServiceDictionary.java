package oscrabble.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import oscrabble.data.IDictionary;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;

public class MicroServiceDictionary implements IDictionary
{
	@Autowired
	public static final RestTemplate REST_TEMPLATE = new RestTemplate();

	private final URI uri;

	/**
	 * Cache for letter information
	 */
	private ScrabbleRules scrabbleRules;

	public MicroServiceDictionary()
	{
		try
		{
			this.uri = new URI("dummy");
		}
		catch (final Throwable throwable)
		{
			throw new Error(throwable);
		}
	}

	public MicroServiceDictionary(final URI uri, final String language)
	{
		this.uri = uri.resolve(language);
	}

	@Override
	public Collection<String> getAdmissibleWords()
	{
		//noinspection ConstantConditions
		return Arrays.asList(REST_TEMPLATE.getForObject(this.uri.resolve("getAdmissibleWords"), String[].class));
	}

	@Override
	public boolean isAdmissible(final String word)
	{
		final ResponseEntity<Object> entity = REST_TEMPLATE.getForEntity(this.uri.resolve("word/").resolve(word), Object.class);
		return entity.getStatusCode() == HttpStatus.OK;
	}

	@Override
	public ScrabbleRules getScrabbleRules()
	{
		if (this.scrabbleRules == null)
		{
			this.scrabbleRules = REST_TEMPLATE.getForObject(this.uri.resolve("getLetterInformation"), ScrabbleRules.class);
		}
		return this.scrabbleRules;
	}

}
