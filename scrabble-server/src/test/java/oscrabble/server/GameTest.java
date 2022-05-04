package oscrabble.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.commons.codec.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.ScrabbleException;
import oscrabble.controller.Action;
import oscrabble.data.Bag;
import oscrabble.data.GameState;
import oscrabble.data.IDictionary;
import oscrabble.data.Player;
import oscrabble.dictionary.Dictionary;
import oscrabble.dictionary.Language;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@SuppressWarnings("HardCodedStringLiteral")
public class GameTest {

	public static final Logger LOGGER = LoggerFactory.getLogger(GameTest.class);
	public static final Random RANDOM = new Random();

	// todo: MOCKEN
	public static IDictionary FRENCH = Dictionary.getDictionary(Language.FRENCH);

	private Game game;
	private UUID gustav;

	@BeforeAll
	public static void mocken() {
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> LOGGER.error(e.toString(), e));
	}

	@BeforeEach
	public void initialize() throws ScrabbleException {
		this.game = new Game(new Server(), FRENCH, -3300078916872261882L);
		this.game.randomPlayerOrder = false;

		final int gameNr = RANDOM.nextInt(100);

		this.gustav = addPlayer("Gustav_" + gameNr);
		addPlayer("second player");
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

	@Test
	@Disabled
	void preparedGame() throws IOException {
		//noinspection ConstantConditions
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
		assertEquals(this.gustav, getPlayerToPlay(this.game).uuid);
		assertEquals(0, this.game.getRoundNr());

		this.game.play(Action.parse(null, "8H APTES"));
		this.game.awaitEndOfPlay(1);
		Assertions.assertNotEquals(this.gustav, getPlayerToPlay(this.game));
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
		Assertions.assertNotEquals(this.gustav, getPlayerToPlay(this.game));

		this.game.rollbackLastMove(this.gustav);
		assertEquals(roundNr, this.game.getRoundNr());
		assertEquals(0, this.game.getPlayer(this.gustav).score);
		assertEquals(this.gustav, getPlayerToPlay(this.game).uuid);
		assertEquals(startRack, this.game.getPlayer(this.gustav).rack);
	}


	@Test
	public void retryForbidden() throws ScrabbleException, InterruptedException, TimeoutException {
		this.game.getConfiguration().setValue("retryAccepted", false);
		this.startGame(true);

		Assertions.assertEquals(this.gustav, getPlayerToPlay(this.game).uuid);
		try {
			this.game.play(Action.parse(this.gustav, "H3 APPETEE"));
			fail();
		} catch (ScrabbleException.ForbiddenPlayException e) {
			// ok
		}
		this.game.awaitEndOfPlay(1);

		assertEquals(this.game.getPlayer(this.gustav).score, 0);
		Assertions.assertNotEquals(this.gustav, getPlayerToPlay(this.game).uuid);
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
		Assertions.assertNotEquals(this.gustav, getPlayerToPlay(this.game));
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
		Assertions.assertNotEquals(this.gustav, getPlayerToPlay(this.game));
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
	@Disabled
	public void testScore() throws ScrabbleException, InterruptedException, TimeoutException {
		this.game = new Game(new Server(), FRENCH, 2346975568742590367L);
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
		}

		{
			// Joker on blue case
			this.game = new Game(new Server(), FRENCH, -6804219371477742897L);
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
		}
	}

	@SneakyThrows
	@Test
	void testScoreCompleteGame() {

		// Game from http://chr.amet.chez-alice.fr/p/commente.htm
		final ArrayList<Pair<String, Integer>> plays = new ArrayList<>();
		plays.add(Pair.of("H4 FATUM", 26));
		plays.add(Pair.of("5E lOUANGEA", 82));
		plays.add(Pair.of("4L VEXA", 44));
		plays.add(Pair.of("8G AMBIANTE", 62));
		plays.add(Pair.of("I8 BUILDING", 64));
		plays.add(Pair.of("O3 FAITES", 63));
		plays.add(Pair.of("O1 DEFAITES", 36));
		plays.add(Pair.of("N2 MIX", 37));
		plays.add(Pair.of("13G WHIP", 28));
		plays.add(Pair.of("K8 AJOURS", 45));
		plays.add(Pair.of("6A LYCEE", 37));
		plays.add(Pair.of("N8 ELUSSENT", 70));
		plays.add(Pair.of("15C HOLDING", 39));
		plays.add(Pair.of("A4 VOLER", 36));
		plays.add(Pair.of("15L BOTE", 27));
		plays.add(Pair.of("M2 ARETE", 35));
		plays.add(Pair.of("D6 ENQuIMES", 96));
		plays.add(Pair.of("A1 SURVOLEREZ", 66));
		plays.add(Pair.of("3A RACKET", 34));
		plays.add(Pair.of("1G PARLOIR", 86));
		testGame(plays);

		plays.clear();
		plays.add(Pair.of("H4 FORGER", 28));
		plays.add(Pair.of("5F EVOQUE", 32));
		plays.add(Pair.of("F4 DEY", 33));
		plays.add(Pair.of("E5 RAGE", 55));
		plays.add(Pair.of("L4 HEIN", 32));
		plays.add(Pair.of("M1 LILAS", 34));
		plays.add(Pair.of("8A bUTTER", 18));
		plays.add(Pair.of("1L CLIN", 27));
		plays.add(Pair.of("B6 JOUTE", 30));
		plays.add(Pair.of("11A BROUM", 29));
		plays.add(Pair.of("M7 IODATES", 75));
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
	
	public synchronized PlayerInformation getPlayerToPlay(Game game) {
		return game.toPlay.getFirst();
	}

}

