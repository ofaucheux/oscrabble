package oscrabble.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class JsonMessage
{
	private Date date;
	private String command;

	/** ID of the game */
	private String game;

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private Map<String, String> parameters;

	static
	{
		MAPPER.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
	}

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
