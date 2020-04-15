package oscrabble.json;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import oscrabble.player.BruteForceMethod;
import oscrabble.player.IPlayer;

import java.io.IOError;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class HttpClient
{
	final CloseableHttpClient client = HttpClients.createDefault();

	private final URI uri;

	private IPlayer player;

	public HttpClient(final URI uri)
	{
		this.uri = uri;
	}

	public UUID publish(final JsonMessage message)
	{
		try
		{
			final HttpPost p = new HttpPost(this.uri);
			p.setEntity(new StringEntity(message.toString()));
			final CloseableHttpResponse response = this.client.execute(p);
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
			{
				throw new IOException(response.toString());
			}
			return UUID.fromString(IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset()));
		}
		catch (IOException e)
		{
			throw new IOError(new IOError(e));
		}
	}

	public JsonMessage readNext(final UUID to, final int maxWait, final TimeUnit maxWaitUnit) throws TimeoutException
	{
		final HttpGet get = new HttpGet(this.uri.resolve("next/" + to));
		get.setHeader(HttpServer.PARAM_MAX_WAIT, Integer.toString(maxWait));
		get.setHeader(HttpServer.PARAM_MAX_WAIT_UNIT, maxWaitUnit.name());
		try
		{
			final CloseableHttpResponse response = this.client.execute(get);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NO_CONTENT)
			{
				throw new TimeoutException();
			}
			return JsonMessage.parse(IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset()));
		}
		catch (IOException e)
		{
			throw new IOError(e);
		}

	}

	public JsonMessage readMessage(final UUID messageId) throws IllegalArgumentException
	{
		try
		{
			final HttpPost post = new HttpPost();
			client.execute(post);
			return null;// TODO
		}
		catch (IOException e)
		{
			throw new IOError(e);
		}
	}
}
