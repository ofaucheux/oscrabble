package oscrabble.json;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Date;

class JsonMessageTest
{

	@Test
	void parse() throws IOException
	{

		JsonMessage message;

		message = getJsonMessage("quitCommand.json");
		Assert.assertEquals(new Date(119, Calendar.DECEMBER, 25, 12, 36, 32), message.getDate());
		Assert.assertNull(message.getParameters());

		message = getJsonMessage("addPlayer.json");
		Assert.assertEquals(1, message.getParameters().size());
		Assert.assertEquals("player1", message.getParameters().get("player"));
	}

	private JsonMessage getJsonMessage(final String filename) throws IOException
	{
		final String json = IOUtils.toString(JsonMessageTest.class.getResourceAsStream("JsonMessageTest/" + filename), Charset.defaultCharset());
		return JsonMessage.parse(json);
	}

}