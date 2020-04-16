package oscrabble.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;

import javax.servlet.http.HttpServletResponse;
import java.io.IOError;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Representation of a message
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "class")
public abstract class JsonMessage
{
	/**
	 * Mapper for serialisation
	 */
	static final ObjectMapper MAPPER = new ObjectMapper();

	static
	{
		MAPPER.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
	}

	/**
	 * creation date
	 */
	private Date date;

	/**
	 * ID of the game, if any
	 */
	private String game;

	/**
	 * ID of the sender.
	 */
	private String from;

	/**
	 * ID of the recipient
	 */
	private String to;

	/**
	 * Parse a message
	 *
	 * @param json the message
	 * @return the parsed message
	 * @throws JsonParseException   if underlying input contains invalid content of type {@link JsonParser} supports (JSON for default case)
	 * @throws JsonMappingException if the input JSON structure does not match structure expected for result type (or has other mismatch issues)
	 */
	public static JsonMessage parse(final String json) throws JsonProcessingException
	{
		return MAPPER.readValue(json, JsonMessage.class);
	}

	public static <A extends JsonMessage> A instantiate(final Class<A> clazz, final String game, final String from, final String to)
	{
		try
		{
			final A m = clazz.getConstructor().newInstance();
			m.setGame(game);
			m.setFrom(from);
			m.setTo(to);
			m.setDate(new Date());
			return m;
		}
		catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e)
		{
			throw new Error(e);
		}
	}

	protected void setDate(final Date date)
	{
		this.date = date;
	}

	public Date getDate()
	{
		return this.date;
	}

	public String getGame()
	{
		return this.game;
	}

	public String getFrom()
	{
		return this.from;
	}

	public String getTo()
	{
		return this.to;
	}

	public String toJson()
	{
		try
		{
			final StringWriter w = new StringWriter();
			MAPPER.writeValue(w, this);
			return w.toString();
		}
		catch (IOException e)
		{
			throw new IOError(e);
		}
	}

	public void setFrom(final String from)
	{
		this.from = from;
	}

	public void setGame(final String game)
	{
		this.game = game;
	}

	public String getCommand()
	{
		return this.getClass().getSimpleName();
	}

	public void setTo(final String to)
	{
		this.to = to;
	}


	/**
	 * Send the message and read the response.
	 *
	 * @param destination
	 * @throws IOScrabbleError
	 */
	public JsonMessage send(final HttpClient sender, final InetSocketAddress destination) throws IOScrabbleError
	{
		try
		{
			final Request request = sender.newRequest(new URI("http://" + destination.getHostString() + ":" + destination.getPort()));
			final String input = toJson();
			request.content(new StringContentProvider(input, "application/json"));
			final ContentResponse response = request.send();
			final String output = response.getContentAsString();
			if (response.getStatus() != HttpServletResponse.SC_OK)
			{
				final StringBuffer sb = new StringBuffer();
				sb.append("Connection with server returned status ").append(response.getStatus()).append("\n");
				sb.append("\nInput:\n").append(input);
				sb.append("\nOuput:\n");
				sb.append(output);
				throw new IOScrabbleError(sb.toString(), null);
			}

			try
			{
				return output == null || output.isEmpty()
						? null
						: JsonMessage.parse(output);
			}
			catch (JsonProcessingException e)
			{
				throw new IOScrabbleError("Cannot parse response\n\n" + output, e);
			}
		}
		catch (URISyntaxException | InterruptedException | TimeoutException | ExecutionException e)
		{
			throw new IOScrabbleError(e.getMessage(), e);
		}
	}
}
