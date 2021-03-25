package oscrabble.json;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Date;

class JsonMessageTest {

	@Test
	void parse() throws IOException {

		JsonMessage message;

		message = getJsonMessage("quitCommand.json");
		Assert.assertEquals(new Date(119, Calendar.DECEMBER, 25, 12, 36, 32), message.getDate());
		Assert.assertEquals("quit", message.getCommand());
		Assert.assertNull(message.getParameters());

		// Parse message
		message = getJsonMessage("addPlayer.json");
		Assert.assertEquals(1, message.getParameters().size());
		Assert.assertEquals("player1", message.getParameters().get("player"));

		// encode message
		Assert.assertEquals(
				JsonMessage.MAPPER.readTree(message.toJson()),
				JsonMessage.MAPPER.readTree(JsonMessageTest.class.getResourceAsStream("JsonMessageTest/" + "addPlayer.json"))
		);

	}

	private JsonMessage getJsonMessage(final String filename) throws IOException {
		final String json = IOUtils.toString(JsonMessageTest.class.getResourceAsStream("JsonMessageTest/" + filename), Charset.defaultCharset());
		return JsonMessage.parse(json);
	}

}