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

	private static final Map<UUID, Context> contexts = new HashMap<>();
	public static final Random RANDOM = new Random();

	final Game game;
	final UUID humanPlayer;
	final Server server;

	public synchronized static Context get(UUID contextId) {
		return contexts.computeIfAbsent(contextId, (key) -> new Context());
	}

	private Context() {
		try {
			this.server = new Server();
			this.game = new Game(this.server, Dictionary.getDictionary(Language.FRENCH), RANDOM.nextInt());

			List<Player> players = new ArrayList<>();
			players.add(Player.builder().name("Human").id(UUID.randomUUID()).build());
			this.humanPlayer = players.get(0).id;

			final Random random = new Random();
			final ArrayList<String> firstNames = NameUtils.getFrenchFirstNames();
			for (int i = 0; i < 2; i++) {
				final String name = firstNames.get(random.nextInt(firstNames.size()));
				final Player player = Player.builder().name(name).id(UUID.randomUUID()).build();
				final AIPlayer ai = new AIPlayer(
						new BruteForceMethod(this.game.getDictionary()),
						this.game.getId(),
						player.getId(),
						this.server
				);
				ai.setThrottle(Duration.ofSeconds(1));
				ai.startDaemonThread();
				players.add(player);
			}

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
