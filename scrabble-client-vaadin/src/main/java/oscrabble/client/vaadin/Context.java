package oscrabble.client.vaadin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.ScrabbleException;
import oscrabble.client.utils.NameUtils;
import oscrabble.data.Player;
import oscrabble.dictionary.Dictionary;
import oscrabble.dictionary.Language;
import oscrabble.player.ai.AIPlayer;
import oscrabble.player.ai.BruteForceMethod;
import oscrabble.server.Game;
import oscrabble.server.Server;

import java.time.Duration;
import java.util.*;

public class Context {
	public static final Logger LOGGER = LoggerFactory.getLogger(Context.class);

	private static Context SINGLETON;

	final Game game;
	final UUID player;
	final Server server;

	public synchronized static Context get() {
		if (SINGLETON == null) {
			SINGLETON = new Context();
		}

		return SINGLETON;
	}

	private Context() {
		try {
			this.server = new Server();
			this.game = new Game(this.server, Dictionary.getDictionary(Language.FRENCH), 2);

			List<Player> players = new ArrayList<>();
			final Random random = new Random();
			final ArrayList<String> firstNames = NameUtils.getFrenchFirstNames();
			for (int i = 0; i < 3; i++) {
				final String name = firstNames.get(random.nextInt(firstNames.size()));
				final Player player = Player.builder().name(name).id(UUID.randomUUID()).build();
				final AIPlayer ai = new AIPlayer(
						new BruteForceMethod(this.game.getDictionary()),
						this.game.getId(),
						player.getId(),
						this.server
				);
				ai.setThrottle(Duration.ofSeconds(3));
				ai.startDaemonThread();
				players.add(player);
			}
			this.player = players.get(0).id;

			for (Player p : players) {
				this.game.addPlayer(p);
			}
			this.game.startGame();
		} catch (ScrabbleException e) {
			LOGGER.error("Error creating context", e);
			throw new Error(e);
		}
	}
}
