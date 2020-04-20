package oscrabble.server;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;

public class MicroServiceDictionary implements IDictionary
{
	public static final RestTemplate REST_TEMPLATE = new RestTemplate();
	private final URI uri;

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
		final ResponseEntity<Object> entity = REST_TEMPLATE.getForEntity(this.uri.resolve("word").resolve(word), Object.class);
		return entity.getStatusCode() == HttpStatus.OK;
	}

	@Override
	public LetterInformation getLetterMetaInfo()
	{
		return REST_TEMPLATE.getForObject(this.uri.resolve("getLetterInformation"), LetterInformation.class);
	}

}
