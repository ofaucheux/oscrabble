package oscrabble.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOError;
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Representation of a message
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonMessage
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
	 * command
	 */
	private String command;

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
	 * Parameters for the call of the function, if any
	 */
	private Map<String, String> parameters;

	/**
	 * Parse a message
	 * @param json the message
	 * @return the parsed message
	 *
	 * @throws JsonParseException if underlying input contains invalid content
	 *    of type {@link JsonParser} supports (JSON for default case)
	 * @throws JsonMappingException if the input JSON structure does not match structure
	 *   expected for result type (or has other mismatch issues)
	 */
	public static JsonMessage parse(final String json) throws JsonProcessingException
	{
		return MAPPER.readValue(json, JsonMessage.class);
	}

	public Date getDate()
	{
		return this.date;
	}

	public String getCommand()
	{
		return this.command;
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

	public Map<String, String> getParameters()
	{
		return this.parameters;
	}

	/**
	 * Create a message.
	 *
	 * @param game
	 * @param to
	 * @param command
	 * @param parameters
	 * @return the new message
	 */
	public static JsonMessage newMessage(final UUID game, final UUID from, final UUID to, final String command, final Map<String, String> parameters)
	{
		final JsonMessage m = new JsonMessage();
		m.game = game.toString();
		m.from = from.toString();
		m.to = to.toString();
		m.command = command;
		m.parameters = new TreeMap<>(parameters);
		return m;
	}

	@Override
	public boolean equals(final Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final JsonMessage that = (JsonMessage) o;
		return Objects.equals(this.date, that.date) &&
				Objects.equals(this.command, that.command) &&
				Objects.equals(this.game, that.game) &&
				Objects.equals(this.from, that.from) &&
				Objects.equals(this.to, that.to) &&
				Objects.equals(this.parameters, that.parameters);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(this.date, this.command, this.game, this.to, this.parameters);
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
}
