package oscrabble.client;

import org.junit.jupiter.api.Test;
import oscrabble.controller.MicroServiceDictionary;
import oscrabble.controller.MicroServiceScrabbleServer;
import oscrabble.player.ai.AIPlayer;
import oscrabble.player.ai.BruteForceMethod;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.*;

class ClientTest
{


	final static MicroServiceDictionary DICTIONARY = new MicroServiceDictionary(URI.create("http://localhost:8080/"), "FRENCH");
	final MicroServiceScrabbleServer server = new MicroServiceScrabbleServer(URI.create("http://localhost:" + MicroServiceScrabbleServer.DEFAULT_PORT));

	@Test
	void displayAll() throws InterruptedException
	{
		final UUID game = this.server.newGame();
		final UUID edgar = this.server.addPlayer(game, "Edgar");
		final UUID anton = this.server.addPlayer(game, "Anton");

		final AIPlayer ai = new AIPlayer(new BruteForceMethod(DICTIONARY), game, anton, server);
		ai.startDaemonThread();

		final Client client = new Client(this.server, game, edgar);
		client.displayAll();
		server.startGame(game);

		do
		{
			Thread.sleep(500);
		} while (client.isVisible());
	}
}