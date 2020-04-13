package oscrabble.json;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

@RunWith(Parameterized.class)
class JsonMessageProviderTest
{
	private final JsonMessageProvider provider;

	public JsonMessageProviderTest(JsonMessageProvider provider)
	{
		this.provider = provider;
	}

	@Test
	void publishAndRead() throws TimeoutException
	{
		final UUID game = UUID.randomUUID();
		final UUID server = UUID.randomUUID();
		final UUID player1 = UUID.randomUUID();
		final UUID player2 = UUID.randomUUID();

		final JsonMessage message = JsonMessage.newMessage(game, player1, server, "getScore", Collections.singletonMap("player", "player2"));
		this.provider.publish(message);
		try
		{
			this.provider.readNext(player1, 500, TimeUnit.MILLISECONDS);
			Assert.fail("No message should have been read");
		}
		catch (TimeoutException e)
		{
			// OK
		}

		final JsonMessage read = this.provider.readNext(game, 1, TimeUnit.SECONDS);
		assertEquals(message, read);
	}
}