package oscrabble.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import oscrabble.ScrabbleError;
import oscrabble.data.Dictionary;
import oscrabble.data.IDictionary;
import oscrabble.data.ScrabbleRules;

import java.net.URI;
import java.util.*;

public class MicroServiceDictionary implements IDictionary
{
	@Autowired
	public static final RestTemplate REST_TEMPLATE = new RestTemplate();

	private final URI uri;
	private final String language;

	/**
	 * Cache for letter information
	 */
	private ScrabbleRules scrabbleRules;

	public MicroServiceDictionary(final URI uri, final String language)
	{
		this.uri = uri;
		this.language = language;
	}

	@Override
	public Collection<String> getAdmissibleWords()
	{
		//noinspection ConstantConditions
		final Dictionary d = REST_TEMPLATE.getForObject(this.uri.resolve(this.language + "/getAdmissibleWords"), Dictionary.class);
		return d.words;
	}

	@Override
	public boolean isAdmissible(final String word)
	{
		final ResponseEntity<Object> entity;
		try
		{
			entity = REST_TEMPLATE.getForEntity(
					this.uri.resolve("/" + this.language + ("/word/") + word),
					Object.class
			);
		}
		catch (HttpClientErrorException e)
		{
			if (e.getStatusCode() == HttpStatus.NOT_FOUND)
			{
				return false;
			}

			throw e;
		}
		return entity.getStatusCode() == HttpStatus.OK;
	}

	@Override
	public ScrabbleRules getScrabbleRules()
	{
		try
		{
			if (this.scrabbleRules == null)
			{
				final URI uri = this.uri.resolve('/' + this.language + "/getScrabbleRules");
				this.scrabbleRules = REST_TEMPLATE.getForObject(uri, ScrabbleRules.class);
			}
			return this.scrabbleRules;
		}
		catch (RestClientException e)
		{
			throw new ScrabbleError("Cannot read scrabble rules", e);
		}
	}

}
