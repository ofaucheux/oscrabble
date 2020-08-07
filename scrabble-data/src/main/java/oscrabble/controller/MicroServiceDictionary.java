package oscrabble.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import oscrabble.ScrabbleError;
import oscrabble.data.Dictionary;
import oscrabble.data.DictionaryEntry;
import oscrabble.data.IDictionary;
import oscrabble.data.ScrabbleRules;

import java.net.URI;
import java.util.Collection;

public class MicroServiceDictionary implements IDictionary
{
	@Autowired
	public static final RestTemplate REST_TEMPLATE = new RestTemplate();
	public static final Logger LOGGER = LoggerFactory.getLogger(MicroServiceDictionary.class);

	private final String language;
	private final UriComponentsBuilder uriComponentsBuilder;

	/**
	 * Cache for letter information
	 */
	private ScrabbleRules scrabbleRules;

	public MicroServiceDictionary(final String host, final int port, final String language)
	{
		this.uriComponentsBuilder = UriComponentsBuilder.newInstance().scheme("http").host(host).port(port);
		this.language = language;
	}

	/**
	 * @return the French dictionary service on localhost with default port
	 */
	public static MicroServiceDictionary getDefaultFrench()
	{
		return new MicroServiceDictionary("localhost", 8080, "FRENCH");
	}

	private synchronized URI buildUri(final String... pathSegments)
	{
		final URI uri = this.uriComponentsBuilder
				.replacePath(this.language)
				.pathSegment(pathSegments)
				.build()
				.toUri();
		LOGGER.trace("Build URI: " + uri);
		return uri;
	}

	@Override
	public Collection<String> getAdmissibleWords()
	{
		final Dictionary d = REST_TEMPLATE.getForObject(buildUri("getAdmissibleWords"), Dictionary.class);
		assert d != null;
		return d.words;
	}

	@SuppressWarnings("unused")
	public DictionaryEntry getEntry(final String word)
	{
		final URI uri = buildUri("getEntry", word);
		return REST_TEMPLATE.getForObject(uri, DictionaryEntry.class);
	}

	@Override
	public boolean isAdmissible(final String word)
	{
		final ResponseEntity<Object> entity;
		try
		{
			entity = REST_TEMPLATE.getForEntity(
					buildUri("word", word),
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
				this.scrabbleRules = REST_TEMPLATE.getForObject(buildUri("getScrabbleRules"), ScrabbleRules.class);
			}
			return this.scrabbleRules;
		}
		catch (RestClientException e)
		{
			throw new ScrabbleError("Cannot read scrabble rules", e);
		}
	}
}
