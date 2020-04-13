package oscrabble.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.lf5.util.StreamUtils;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

class JsonMessageTest
{

	@Test
	void parse() throws IOException
	{
		final String json = IOUtils.toString(JsonMessageTest.class.getResourceAsStream("JsonMessageTest/quitCommand.json"));
		final JsonMessage message = JsonMessage.parse(json);
		Assert.assertEquals(new Date(119, Calendar.DECEMBER, 25, 12, 36, 32), message.getDate());
	}
}