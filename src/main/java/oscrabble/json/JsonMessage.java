package oscrabble.json;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Representation of a message
 */
public class JsonMessage
{
	/**
	 * Mapper for serialisation
	 */
	private static final ObjectMapper MAPPER = new ObjectMapper();
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

	public Map<String, String> getParameters()
	{
		return this.parameters;
	}
}
