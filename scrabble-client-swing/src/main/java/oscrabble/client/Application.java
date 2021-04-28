package oscrabble.client;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.client.ui.ConnectionParameterPanel;
import oscrabble.client.utils.Starter;
import oscrabble.controller.MicroServiceDictionary;
import oscrabble.controller.MicroServiceScrabbleServer;
import oscrabble.player.ai.AIPlayer;
import oscrabble.player.ai.BruteForceMethod;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.*;
import java.util.List;

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
				new ConnectionParameterPanel(connectionParameters)
		);

		if (connectionParameters.localServer) {
			final Starter starter = new Starter();
			final JDialog dialog = new JDialog();
			dialog.setLayout(new BorderLayout());
			final JPanel panel = starter.getPanel();
			panel.setBorder(new TitledBorder("Start the servers..."));
			dialog.add(panel);
			dialog.setMinimumSize(new Dimension(300, 100));
			dialog.setLocationRelativeTo(null);
			dialog.pack();
			new Thread(() -> {
				try {
					starter.startApplications(true);
					Thread.sleep(3000);
					dialog.dispose();
				} catch (Throwable e) {
					LOGGER.error(e.toString(), e);
					dialog.dispose();
					JOptionPane.showMessageDialog(null, "Error occurred, see log.");
				}
			}).start();
			dialog.setModal(true);
			dialog.setVisible(true);
		}

		final MicroServiceDictionary dictionary = MicroServiceDictionary.getDefaultFrench();
		final MicroServiceScrabbleServer server = connectionParameters.localServer
				? MicroServiceScrabbleServer.getLocal()
				: new MicroServiceScrabbleServer(connectionParameters.serverName, connectionParameters.serverPort);

		//
		// start the application
		//

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
		final HashSet<AIPlayer> aiPlayers = new HashSet<>();
		for (int i = 0; i < (Integer.parseInt((String) this.properties.get("players.number"))) - 1; i++) {
			final UUID anton = this.server.addPlayer(game, names.get(i + 1));
			// TODO: tell the server it is an AI Player
			final AIPlayer ai = new AIPlayer(new BruteForceMethod(this.dictionary), game, anton, this.server);
			ai.setThrottle(Duration.ofSeconds(1));
			ai.startDaemonThread();
			aiPlayers.add(ai);
		}

		final Client client = new Client(this.server, game, edgar);
		client.setAIPlayers(aiPlayers);
		client.displayAll();
		this.server.startGame(game);

		do {
			Thread.sleep(500);
		} while (client.isVisible());
		this.server.attach(game, edgar, true);
	}

	@Data
	public static class ConnectionParameters {
		int serverPort;
		String serverName;
		private boolean localServer;
	}
}
