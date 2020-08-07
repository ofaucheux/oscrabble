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

@SuppressWarnings("BusyWait")
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

		Thread.setDefaultUncaughtExceptionHandler((t, e) -> LOGGER.error("Uncaught exception", e));

		final  MicroServiceDictionary dictionary = MicroServiceDictionary.getDefaultFrench();
		final  MicroServiceScrabbleServer server = MicroServiceScrabbleServer.getLocal();
		final Application application = new Application(dictionary, server);
		application.play();
	}

	private void play() throws InterruptedException
	{
		final UUID game = this.server.newGame();
		final UUID edgar = this.server.addPlayer(game, "Edgar");
		for (int i = 0; i < 4; i++)
		{
			final UUID anton = this.server.addPlayer(game, "Anton_" + (i+1));
			// TODO: tell the server it is an AI Player
			final AIPlayer ai = new AIPlayer(new BruteForceMethod(this.dictionary), game, anton, this.server);
			ai.setThrottle(1000);
			ai.startDaemonThread();
		}

		final Client client = new Client(this.server, game, edgar);
		client.displayAll();
		this.server.startGame(game);

		do
		{
			Thread.sleep(500);
		} while (client.isVisible());
		this.server.attach(game, edgar, true);
	}

}
