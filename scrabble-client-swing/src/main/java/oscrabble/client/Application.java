package oscrabble.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.controller.MicroServiceDictionary;
import oscrabble.controller.MicroServiceScrabbleServer;
import oscrabble.player.ai.AIPlayer;
import oscrabble.player.ai.BruteForceMethod;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.UUID;

public class Application
{
	public static final Logger LOGGER = LoggerFactory.getLogger(Thread.class);

	private final MicroServiceDictionary dictionary;
	private final MicroServiceScrabbleServer server;

	/**
	 * Resource Bundle
	 */
	public final static ResourceBundle MESSAGES = ResourceBundle.getBundle("Messages", new Locale("fr_FR"));

	public Application(final MicroServiceDictionary dictionary, final MicroServiceScrabbleServer server)
	{
		this.dictionary = dictionary;
		this.server = server;
	}

	public static void main(String[] unused) throws InterruptedException
	{

		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			public void uncaughtException(Thread t, Throwable e)
			{
				LOGGER.error("Uncaught exception", e);
			}
		});

		final  MicroServiceDictionary dictionary = new MicroServiceDictionary("localhost", 8080, "FRENCH");
		final  MicroServiceScrabbleServer server = new MicroServiceScrabbleServer("localhost", MicroServiceScrabbleServer.DEFAULT_PORT);
		final Application application = new Application(dictionary, server);
		application.play();
	}

	private void play() throws InterruptedException
	{
		final UUID game = this.server.newGame();
		final UUID edgar = this.server.addPlayer(game, "Edgar");
		final UUID anton = this.server.addPlayer(game, "Anton");

		final AIPlayer ai = new AIPlayer(new BruteForceMethod(this.dictionary), game, anton, this.server);
		ai.setThrottle(5000);
		ai.startDaemonThread();

		final Client client = new Client(this.server, game, edgar);
		client.displayAll();
		this.server.startGame(game);

		do
		{
			Thread.sleep(500);
		} while (client.isVisible());
	}

}
