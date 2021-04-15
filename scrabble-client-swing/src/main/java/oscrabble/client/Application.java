package oscrabble.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.controller.MicroServiceDictionary;
import oscrabble.controller.MicroServiceScrabbleServer;
import oscrabble.player.ai.AIPlayer;
import oscrabble.player.ai.BruteForceMethod;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@SuppressWarnings("BusyWait")
public class Application {
	public static final Logger LOGGER = LoggerFactory.getLogger(Thread.class);

	/**
	 * Resource Bundle
	 */
	public final static ResourceBundle MESSAGES = ResourceBundle.getBundle("Messages", new Locale("fr_FR"));

	private final MicroServiceDictionary dictionary;
	private final MicroServiceScrabbleServer server;
	private final Properties properties;

	public Application(final MicroServiceDictionary dictionary, final MicroServiceScrabbleServer server, final Properties properties) {
		this.dictionary = dictionary;
		this.server = server;
		this.properties = properties;
	}

	public static void main(String[] unused) throws InterruptedException, IOException {

		Thread.setDefaultUncaughtExceptionHandler((t, e) -> LOGGER.error("Uncaught exception", e));

		final MicroServiceDictionary dictionary = MicroServiceDictionary.getDefaultFrench();
		final MicroServiceScrabbleServer server = MicroServiceScrabbleServer.getLocal();
		final Properties properties = new Properties();
		try (final InputStream resource = Application.class.getResourceAsStream("client.properties")) {
			properties.load(resource);
		}
		final Application application = new Application(dictionary, server, properties);
		application.playGame();
	}

	final private static List<String> POSSIBLE_PLAYER_NAMES = Arrays.asList("Philipp",
			"Emil",
			"Thomas",
			"Romain",
			"Alix",
			"Paul",
			"Batiste",
			"Noemie",
			"Laurent"
	);

	/**
	 * Prepare the server and the client, start the game and play it till it ends.
	 * @throws InterruptedException
	 */
	private void playGame() throws InterruptedException {
		final List<String> names = new ArrayList<>(POSSIBLE_PLAYER_NAMES);
		Collections.shuffle(names);

		final UUID game = this.server.newGame();
		final UUID edgar = this.server.addPlayer(game, names.get(0));
		for (int i = 0; i < (Integer.parseInt((String) this.properties.get("players.number"))) - 1; i++) {
			final UUID anton = this.server.addPlayer(game, names.get(i + 1));
			// TODO: tell the server it is an AI Player
			final AIPlayer ai = new AIPlayer(new BruteForceMethod(this.dictionary), game, anton, this.server);
			ai.startDaemonThread();
		}

		final Client client = new Client(this.server, game, edgar);
		client.displayAll();
		this.server.startGame(game);

		do {
			Thread.sleep(500);
		} while (client.isVisible());
		this.server.attach(game, edgar, true);
	}
}
