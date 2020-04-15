package oscrabble.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOError;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.Date;

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

	public static JsonMessage instantiate(final Class<? extends JsonMessage> clazz, final String game, final String from, final String to)
	{
		try
		{
			final JsonMessage m = clazz.getConstructor().newInstance();
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

//	enum Command
//	{
//		POLL_MESSAGE,
//
//		// Commands send to a player
//		EDIT_PARAMETERS,
//		GET_CONFIGURATION,
//		GET_NAME,
//		GET_PLAYER_KEY,
//		GET_TYPE,
//		HAS_EDITABLE_PARAMETERS,
//		IS_OBSERVER,
//		SET_PLAYER_KEY,
//		SET_GAME,
//
//		// Commands send to a server
//		SET_STATE,
//		ADD_PLAYER,
//		PLAY,
//		GET_PLAYER_TO_PLAY,
//		GET_PLAYERS,
//		GET_HISTORY,
//		GET_GRID,
//		GET_RACK,
//		GET_SCORE,
//		QUIT,
//		IS_LAST_PLAY_ERROR,
//		ROLLBACK_LAST_MOVE,
//		PLAYER_CONFIG_HAS_CHANGED,
//		GET_REQUIRED_TILES_IN_BAG_FOR_EXCHANGE,
//	}
}
