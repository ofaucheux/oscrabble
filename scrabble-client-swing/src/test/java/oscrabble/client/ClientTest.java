package oscrabble.client;

import org.junit.jupiter.api.Test;
import oscrabble.controller.MicroServiceDictionary;
import oscrabble.controller.MicroServiceScrabbleServer;

import java.net.URI;
import java.util.UUID;

class ClientTest
{


	final MicroServiceDictionary DICTIONARY = new MicroServiceDictionary(URI.create("http://localhost:8080/"), "FRENCH");
	final MicroServiceScrabbleServer server = new MicroServiceScrabbleServer(URI.create("http://localhost:" + MicroServiceScrabbleServer.DEFAULT_PORT));

	@Test
	void displayAll() throws InterruptedException
	{
		final UUID game = this.server.newGame();
		final UUID edgar = this.server.addPlayer(game, "Edgar");
		final Client client = new Client(this.server, game, edgar);
		client.displayAll();

		do
		{
			Thread.sleep(100);
		} while (client.isVisible());
	}
}