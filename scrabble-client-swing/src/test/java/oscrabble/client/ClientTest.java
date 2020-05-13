package oscrabble.client;

import org.junit.jupiter.api.Test;
import oscrabble.controller.MicroServiceDictionary;
import oscrabble.controller.MicroServiceScrabbleServer;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.*;

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

		final ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
		service.schedule(() -> server.startGame(game), 3, TimeUnit.SECONDS);

		do
		{
			Thread.sleep(500);
		} while (client.isVisible());
	}
}