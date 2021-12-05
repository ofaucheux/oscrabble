package oscrabble.server;

import oscrabble.ScrabbleException;
import oscrabble.controller.ScrabbleServerInterface;
import oscrabble.data.*;

import java.util.*;

public class Server implements ScrabbleServerInterface {

	private final TreeSet<String> refusedWords = new TreeSet<>();

	/**
	 * Get an already known game.
	 *
	 * @param uuid id
	 * @return the game with this id
	 * @throws ScrabbleException if no search game.
	 */
	static Game getGame(UUID uuid) throws ScrabbleException {
		return Game.getGame(uuid);
	}

	@Override
	public List<Score> getScores(final UUID game, final Collection<String> notations) throws ScrabbleException {
		return getGame(game).getScores(new ArrayList<>(notations));
	}

	@Override
	public GameState getState(final UUID game) throws ScrabbleException {
		return getGame(game).getGameState();
	}

	@Override
	public void acknowledgeState(final UUID game, final UUID player, final GameState state) throws ScrabbleException {
		getGame(game).acknowledge(player);
	}

	@Override
	public Bag getRack(final UUID game, final UUID player) throws ScrabbleException {
		return getGame(game).getPlayer(player).rack;
	}

	@Override
	public PlayActionResponse play(final UUID game, final Action action) throws ScrabbleException, InterruptedException {
		Game g = null;
		final PlayActionResponse.PlayActionResponseBuilder aBuilder = PlayActionResponse.builder().action(action);
		boolean retryAccepted = false;
		try {
			g = getGame(game);
			retryAccepted = g.isRetryAccepted();
			g.play(action);
			aBuilder.success(true).message("success");
		} catch (ScrabbleException | InterruptedException e) {
			aBuilder.success(false).message(e.toString());
		}
		aBuilder.gameState(g == null ? null : g.getGameState())
			.retryAccepted(retryAccepted);
		return aBuilder.build();
	}

	@Override
	public UUID newGame() {
		final Game game = new Game(this, Game.DICTIONARY, new Random().nextLong());
		return game.id;
	}

	@Override
	public UUID addPlayer(final UUID game, final String name) throws ScrabbleException {
		final Player p = Player.builder().id(UUID.randomUUID()).name(name).build();
		final PlayerInformation pi = getGame(game).addPlayer(p);
		return pi.uuid;
	}

	@Override
	public void startGame(final UUID game) throws ScrabbleException {
		getGame(game).startGame();
	}

	@Override
	public ScrabbleRules getRules(final UUID game) throws ScrabbleException {
		return getGame(game).getScrabbleRules();
	}

	@Override
	public void attach(final UUID game, final UUID player, final boolean attach) throws ScrabbleException {
		getGame(game).updatePlayer(PlayerUpdateRequest.createAttachRequest(player, attach));
	}

	@Override
	public void addRefusedWord(final UUID game, final String refusedWord) {
		// remarks: we could check the game, but don't for the moment
		this.refusedWords.add(refusedWord.toUpperCase());
	}

	@Override
	public void setAdditionalRefusedWords(final UUID gameId, final Set<String> refusedWords) throws ScrabbleException {
		if (refusedWords.equals(this.refusedWords)) {
			return;
		}

		this.refusedWords.clear();
		refusedWords.forEach(w -> this.refusedWords.add(w.toUpperCase(Locale.ROOT)));
	}

	@Override
	public Set<String> getAdditionalRefusedWords(final UUID game) {
		return Collections.unmodifiableSet(this.refusedWords);
	}

	public boolean isRefused(final UUID game, final String word) {
		return this.refusedWords.contains(word.toUpperCase());
	}

//	/**
//	 * Let the server loads the fixture games and return them.
//	 *
//	 * @return
//	 */
//	public List<GameState> loadFixtures() {
//		final GameState[] games = REST_TEMPLATE.postForObject(resolve(null, "loadFixtures"), null, GameState[].class);
//		assert games != null;
//		return Arrays.asList(games);
//	}

}
