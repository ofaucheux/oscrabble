package oscrabble.client;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.ScrabbleException;
import oscrabble.client.ui.ConnectionParameterPanel;
import oscrabble.client.utils.I18N;
import oscrabble.client.utils.NameUtils;
import oscrabble.controller.ScrabbleServerInterface;
import oscrabble.data.IDictionary;
import oscrabble.dictionary.Dictionary;
import oscrabble.dictionary.Language;
import oscrabble.player.ai.AIPlayer;
import oscrabble.player.ai.BruteForceMethod;
import oscrabble.server.Server;

import javax.swing.*;
import java.io.*;
import java.time.Duration;
import java.util.*;
import java.util.List;

@SuppressWarnings("BusyWait")
public class Application {
	public static final Logger LOGGER = LoggerFactory.getLogger(Thread.class);
	private static final Random RANDOM = new Random();

	private final IDictionary dictionary;
	private final ScrabbleServerInterface server;
	private final Properties properties;

	private static Application singleton;
	private UUID gameId;

	public Application(final IDictionary dictionary, final ScrabbleServerInterface server, final Properties properties) {
		if (singleton != null) {
			throw new IllegalStateException("Singleton already set");
		}
		this.dictionary = dictionary;
		this.server = server;
		this.properties = properties;
		singleton = this;
	}

	public static void main(String[] unused) throws InterruptedException, IOException, ScrabbleException {

		Thread.setDefaultUncaughtExceptionHandler((t, e) -> LOGGER.error("uncaught.exception", e));

		initializeLogging();

		//
		// load the properties
		//

		final Properties properties = new Properties();
		try (final InputStream resource = Application.class.getResourceAsStream("client.properties")) {
			properties.load(resource);
		}

		//
		// select the server
		//

		final ConnectionParameters connectionParameters = new ConnectionParameters();
		JOptionPane.showMessageDialog(
				null,
				new ConnectionParameterPanel(connectionParameters),
				I18N.get("connection.panel.title"),
				JOptionPane.PLAIN_MESSAGE
		);

		final IDictionary dictionary = Dictionary.getDictionary(Language.FRENCH);
		final ScrabbleServerInterface server   // todo new MicroServiceScrabbleServer(connectionParameters.serverName, connectionParameters.serverPort);
				;
		server = connectionParameters.localServer
				? new Server()
				: null; // todo

		//
		// start the application
		//

		final Application application = new Application(dictionary, server, properties);
		application.playGame();
	}

	/**
	 *
	 */
	private static void initializeLogging() {
		final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		try {
			JoranConfigurator configurator = new JoranConfigurator();
			configurator.setContext(context);
			// Call context.reset() to clear any previous configuration, e.g. default
			// configuration. For multi-step configuration, omit calling context.reset().
			context.reset();
			configurator.doConfigure(Application.class.getResourceAsStream("/logback.xml")); //NON-NLS
		} catch (JoranException je) {
			// StatusPrinter will handle this
		}
		StatusPrinter.printInCaseOfErrorsOrWarnings(context);
	}

	public static UUID getGameId() {
		return singleton == null ? null : singleton.gameId;
	}

	/**
	 * @return p.ex. "v1.0.22-SNAPSHOT"
	 */
	public static String getFormattedVersion() {
		final String version = Application.class.getPackage().getImplementationVersion();
		return version == null ? "-" : "v" + version; //NON-NLS
	}

	/**
	 * Prepare the server and the client, start the game and play it till it ends.
	 * @throws InterruptedException
	 */
	private void playGame() throws InterruptedException, ScrabbleException {
		gameId = this.server.newGame();
		final String humanName = System.getProperty("user.name");
		final UUID humanPlayer = this.server.addPlayer(gameId, humanName);

		final List<String> names = NameUtils.getFrenchFirstNames();
		names.remove(humanName);
		final HashSet<AIPlayer> aiPlayers = new HashSet<>();
		for (int i = 0; i < (Integer.parseInt((String) this.properties.get("players.number"))) - 1; i++) {
			final String aiPlayerName = names.get(RANDOM.nextInt(names.size()));
			names.remove(aiPlayerName);
			final UUID aiPlayerId = this.server.addPlayer(gameId, aiPlayerName);
			// TODO: tell the server it is an AI Player
			final AIPlayer ai = new AIPlayer(new BruteForceMethod(this.dictionary), gameId, aiPlayerId, this.server);
			ai.setThrottle(Duration.ofSeconds(1));
			ai.startDaemonThread();
			aiPlayers.add(ai);
		}

		final Client client = new Client(this.server, this.dictionary, gameId, humanPlayer);
		client.setAIPlayers(aiPlayers);
		client.displayAll();
		this.server.startGame(gameId);

		do {
			Thread.sleep(500);
		} while (client.isVisible());
		this.server.attach(gameId, humanPlayer, true);
	}

	@Data
	public static class ConnectionParameters {
		int serverPort;
		String serverName;
		private boolean localServer;
	}
}
