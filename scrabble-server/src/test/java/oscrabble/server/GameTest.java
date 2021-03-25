package oscrabble.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.util.Pair;
import lombok.SneakyThrows;
import org.apache.commons.codec.Charsets;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.ScrabbleException;
import oscrabble.controller.Action;
import oscrabble.controller.MicroServiceDictionary;
import oscrabble.data.Bag;
import oscrabble.data.GameState;
import oscrabble.data.IDictionary;
import oscrabble.data.Player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Queue;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GameTest {

	public static final Logger LOGGER = LoggerFactory.getLogger(GameTest.class);
	public static final Random RANDOM = new Random();
	public static IDictionary FRENCH = MicroServiceDictionary.getDefaultFrench();

	private Game game;
	private UUID gustav;

	@BeforeAll
	public static void mocken() {
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> LOGGER.error(e.toString(), e));
	}

	@BeforeEach
	public void initialize() throws ScrabbleException {
		this.game = new Game(FRENCH, -3300078916872261882L);
		this.game.randomPlayerOrder = false;

		final int gameNr = RANDOM.nextInt(100);

		this.gustav = addPlayer("Gustav_" + gameNr);
	}

	/**
	 * Create and add a player in the game
	 *
	 * @param name name of the player
	 * @return this player
	 */
	private UUID addPlayer(final String name) throws ScrabbleException {
		final Player player = Player.builder()
				.name(name)
				.id(UUID.randomUUID())
				.build();
		this.game.addPlayer(player);
		return player.id;
	}

	@AfterEach
	public void endsGame() {
		this.game.quitGame();
	}

	@Test
	void preparedGame() throws IOException {
		final String fixture = IOUtils.toString(GameTest.class.getResourceAsStream("game_1.json"), Charsets.UTF_8);
		final GameState gameState = new ObjectMapper().readValue(fixture, GameState.class);
		new Game(gameState);
	}

	@Test // TODO: reactivate retry
	@Disabled
	public void retryAccepted() throws ScrabbleException, InterruptedException, TimeoutException {
		// test retry accepted
		this.game.getConfiguration().setValue("retryAccepted", true);
		this.game.assertFirstLetters("APPESTEE");
		this.startGame(true);
		assertEquals(0, this.game.getRoundNr());

		this.game.play(Action.parse(null, "H3 APPETQE"));
		Thread.sleep(100);
		assertEquals(this.game.getPlayer(this.gustav).score, 0);
		assertEquals(this.gustav, this.game.getPlayerToPlay().uuid);
		assertEquals(0, this.game.getRoundNr());

		this.game.play(Action.parse(null, "8H APTES"));
		this.game.awaitEndOfPlay(1);
		Assertions.assertNotEquals(this.gustav, this.game.getPlayerToPlay());
		assertEquals(16, this.game.getPlayer(this.gustav).score);
	}

	@Test
	@Disabled // todo: reimplement rollback
	public void rollback() throws ScrabbleException, InterruptedException, TimeoutException {
		this.game.assertFirstLetters("APTESSIF");
		this.startGame(true);

		final int roundNr = this.game.getRoundNr();
		final Bag startRack = this.game.getPlayer(this.gustav).rack;
		this.game.play(Action.parse(null, "8H APTES"));
		this.game.awaitEndOfPlay(1);
		assertEquals(16, this.game.getPlayer(this.gustav).score);
		Assertions.assertNotEquals(this.gustav, this.game.getPlayerToPlay());

		this.game.rollbackLastMove(this.gustav);
		assertEquals(roundNr, this.game.getRoundNr());
		assertEquals(0, this.game.getPlayer(this.gustav).score);
		assertEquals(this.gustav, this.game.getPlayerToPlay().uuid);
		assertEquals(startRack, this.game.getPlayer(this.gustav).rack);
	}


	@Test
	@Disabled // todo: reimplement retry
	public void retryForbidden() throws ScrabbleException, InterruptedException, TimeoutException {
		this.game.getConfiguration().setValue("retryAccepted", false);
		final AtomicBoolean playRejected = new AtomicBoolean(false);
		final TestListener listener = new TestListener() {
			@Override
			public void afterRejectedAction(final PlayerInformation player, final Action action) {
			}

			{
				playRejected.set(true);
			}
		};
		this.game.listeners.add(listener);
		this.startGame(true);
		this.game.play(Action.parse(null, "H3 APPETEE"));
		this.game.awaitEndOfPlay(1);

		Assertions.assertTrue(playRejected.get());
		assertEquals(this.game.getPlayer(this.gustav).score, 0);
		Assertions.assertNotEquals(this.gustav, this.game.getPlayerToPlay());
	}

	@Test
	public void startWithOnlyOneLetter() throws ScrabbleException, InterruptedException {
		this.game.getConfiguration().setValue("retryAccepted", false);
		this.game.assertFirstLetters("A");
		startGame(true);
		try {
			this.game.play(Action.parse(null, "H8 A"));
			Assertions.fail();
		} catch (ScrabbleException.ForbiddenPlayException e) {
			// OK
		}
		Assertions.assertNotEquals(this.gustav, this.game.getPlayerToPlay());
	}


	@Test
	public void startNotCentered() throws ScrabbleException, InterruptedException, TimeoutException {
		this.game.getConfiguration().setValue("retryAccepted", false);
		startGame(true);
		try {
			this.game.play(Action.parse(null, "G7 AS"));
			this.game.awaitEndOfPlay(1);
			Assertions.fail();
		} catch (ScrabbleException.ForbiddenPlayException e) {
			// OK
		}
		Assertions.assertNotEquals(this.gustav, this.game.getPlayerToPlay());
	}

	@Test
	public void wordDoesNotTouch() throws ScrabbleException, InterruptedException {
		this.game.getConfiguration().setValue("retryAccepted", false);
		this.game.assertFirstLetters("ASWEEDVIGIE");
		startGame(true);
		play(this.game, "H8 AS");
		try {
			play(this.game, "A3 VIGIE");
			Assertions.fail();
		} catch (ScrabbleException e) {
			// ok
		}
	}

	/**
	 * Plays an action without player
	 *
	 * @param game
	 * @param action
	 * @throws ScrabbleException
	 */
	void play(final Game game, final String action) throws ScrabbleException {
		final ScoreCalculator.MoveMetaInformation mi = ScoreCalculator.getMetaInformation(
				game.grid,
				game.scrabbleRules,
				((Action.PlayTiles) Action.parse(null, action))
		);
		game.play(mi);
	}

	@Test
	public void testScore() throws ScrabbleException, InterruptedException, TimeoutException {
		this.game = new Game(FRENCH, 2346975568742590367L);
		this.game.waitAcknowledges = false;
		this.game.assertFirstLetters("FTINOA ");

		final UUID etienne = addPlayer("Etienne");
		startGame(true);

		this.game.play(Action.parse(etienne, "H8 As"));
		this.game.awaitEndOfPlay(1);
		assertEquals(2, this.game.getPlayer(etienne).score);

		this.game.play(Action.parse(etienne, "8I SI"));
		this.game.awaitEndOfPlay(2);
		assertEquals(4, this.game.getPlayer(etienne).score);

		{
			// Joker on normal case
			this.game = new Game(FRENCH);
			final UUID anton = addPlayer("anton");
			this.game.assertFirstLetters(" CELMNPIERA");

			startGame(true);
			int move = 1;
			this.game.play(Action.parse(null, "D8 PLaCE"));
			this.game.awaitEndOfPlay(move++);
			assertEquals(22, this.game.getPlayer(anton).score);
			this.game.play(Action.parse(null, RANDOM.nextBoolean() ? "4F NIERa" : "4F NIERA"));
			this.game.awaitEndOfPlay(move);
			assertEquals(28, this.game.getPlayer(anton).score);
			this.game.quitGame();
		}

		{
			// Joker on blue case
			this.game = new Game(FRENCH, -6804219371477742897L);
			final UUID anton = addPlayer("Anton");
			this.game.assertFirstLetters(" CELMNPAISSE");
			startGame(true);
			int move = 1;
			this.game.play(Action.parse(null, "D8 aMPLE"));
			this.game.awaitEndOfPlay(move++);
			assertEquals(14, this.game.getPlayer(anton).score);
			this.game.play(Action.parse(null, "7D CAISSE"));
			this.game.awaitEndOfPlay(move);
			assertEquals(28, this.game.getPlayer(anton).score);
			this.game.quitGame();
		}
	}

	@SneakyThrows
	@Test
	void testScoreCompleteGame() {

		// Game from http://chr.amet.chez-alice.fr/p/commente.htm
		final ArrayList<Pair<String, Integer>> plays = new ArrayList<>();
		plays.add(new Pair<>("H4 FATUM", 26));
		plays.add(new Pair<>("5E lOUANGEA", 82));
		plays.add(new Pair<>("4L VEXA", 44));
		plays.add(new Pair<>("8G AMBIANTE", 62));
		plays.add(new Pair<>("I8 BUILDING", 64));
		plays.add(new Pair<>("O3 FAITES", 63));
		plays.add(new Pair<>("O1 DEFAITES", 36));
		plays.add(new Pair<>("N2 MIX", 37));
		plays.add(new Pair<>("13G WHIP", 28));
		plays.add(new Pair<>("K8 AJOURS", 45));
		plays.add(new Pair<>("6A LYCEE", 37));
		plays.add(new Pair<>("N8 ELUSSENT", 70));
		plays.add(new Pair<>("15C HOLDING", 39));
		plays.add(new Pair<>("A4 VOLER", 36));
		plays.add(new Pair<>("15L BOTE", 27));
		plays.add(new Pair<>("M2 ARETE", 35));
		plays.add(new Pair<>("D6 ENQuIMES", 96));
		plays.add(new Pair<>("A1 SURVOLEREZ", 66));
		plays.add(new Pair<>("3A RACKET", 34));
		plays.add(new Pair<>("1G PARLOIR", 86));
		testGame(plays);

		plays.clear();
		plays.add(new Pair<>("H4 FORGER", 28));
		plays.add(new Pair<>("5F EVOQUE", 32));
		plays.add(new Pair<>("F4 DEY", 33));
		plays.add(new Pair<>("E5 RAGE", 55));
		plays.add(new Pair<>("L4 HEIN", 32));
		plays.add(new Pair<>("M1 LILAS", 34));
		plays.add(new Pair<>("8A bUTTER", 18));
		plays.add(new Pair<>("1L CLIN", 27));
		plays.add(new Pair<>("B6 JOUTE", 30));
		plays.add(new Pair<>("11A BROUM", 29));
		plays.add(new Pair<>("M7 IODATES", 25));
		testGame(plays);
	}

	private void testGame(final ArrayList<Pair<String, Integer>> plays) throws ScrabbleException, InterruptedException {
		final Game game = new Game(FRENCH);
		game.setTestModus(true);
		game.startGame();

		for (final Pair<String, Integer> play : plays) {
			final oscrabble.data.Action action = oscrabble.data.Action.builder()
					.player(null)
					.notation(play.getKey())
					.build();
			game.play(action);
			final Action last = game.history.get(game.history.size() - 1);
			assertEquals(play.getValue(), last.score, "Wrong score for \"" + play.getKey() + "\" false");
		}
	}

	/**
	 * Start the game.
	 *
	 * @param fork on a new thread if true.
	 */
	public void startGame(final boolean fork) throws ScrabbleException, InterruptedException {
		this.game.listeners.add(new TestListener() {
			@Override
			public void onGameStateChanged() {
				LOGGER.info("Game state changed to " + GameTest.this.game.getState().name());
			}
		});
		if (fork) {
			final AtomicReference<ScrabbleException> exception = new AtomicReference<>();
			new Thread(() -> this.game.startGame()).start();
			Thread.sleep(300);
			if (exception.get() != null) {
				throw exception.get();
			}
		} else {
			this.game.startGame();
		}

	}

	/**
	 * Default listener. Does nothing.
	 */
	abstract static class TestListener implements GameListener {

		private final ArrayBlockingQueue<ScrabbleEvent> queue = new ArrayBlockingQueue<>(8);
		private final AtomicReference<Throwable> thrownException;

		TestListener() {
			this.thrownException = new AtomicReference<>();
			final Thread thread = new Thread(() -> {
				try {
					while (true) {
						final ScrabbleEvent event;
						event = TestListener.this.queue.take();
						event.accept(this);
					}
				} catch (final Throwable e) {
					this.thrownException.set(e);
				}
			});
			thread.setDaemon(true);
			thread.setName("Test listener");
			thread.start();
		}

		@Override
		public Queue<ScrabbleEvent> getIncomingEventQueue() {
			return this.queue;
		}
	}
}

