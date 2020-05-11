package oscrabble.controller;

import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import oscrabble.ScrabbleException;
import oscrabble.data.GameState;
import oscrabble.data.Player;
import oscrabble.data.objects.Grid;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class MicroServiceScrabbleServer
{
	/**
	 * Default port of scrabble servers
	 */
	public static final int DEFAULT_PORT = 2511;

	@Autowired
	private static final RestTemplate REST_TEMPLATE = new RestTemplate();
	public static final Logger LOGGER = LoggerFactory.getLogger(MicroServiceScrabbleServer.class);

	/**
	 * uri of the server
	 */
	private final URI uri;

	/**
	 * @param uri uri of the server
	 */
	public MicroServiceScrabbleServer(final URI uri)
	{
		this.uri = uri;
	}


	/**
	 * Create a game
	 * @return id of the created new game
	 */
	public UUID newGame()
	{
		final GameState state = REST_TEMPLATE.postForObject(resolve(null, "newGame"), null, GameState.class);
		return state.gameId;
	}

	/**
	 * @return state of the game
	 * @throws ScrabbleException.CommunicationException
	 */
	public GameState getState(final UUID game) throws ScrabbleException.CommunicationException
	{
		final ResponseEntity<GameState> re = REST_TEMPLATE.getForEntity(resolve(game, "getState"), GameState.class);
		if (!re.getStatusCode().is2xxSuccessful())
		{
			throw new ScrabbleException.CommunicationException("Cannot get state: " + re.getStatusCode().getReasonPhrase());
		}
		final GameState gameState = re.getBody();
		//noinspection ConstantConditions
		LOGGER.info(gameState.toString());
		return gameState;
	}

	/**
	 * @return id and name of the player on turn.
	 * @throws ScrabbleException.CommunicationException -
	 */
	public Pair<UUID, String> getPlayerOnTurn(final UUID game) throws ScrabbleException.CommunicationException
	{
		final GameState state = getState(game);
		final UUID uuid = state.getPlayerOnTurn();
		return uuid == null
				? null
				: new Pair<>(uuid, listPlayer(state).get(uuid).name);
	}

	/**
	 * Extract the list of the players
	 * @param state state of the game
	 * @return mapping player id > player.
	 */
	private static Map<UUID, Player> listPlayer(final GameState state)
	{
		final LinkedHashMap<UUID, Player> map = new LinkedHashMap<>();
		state.players.forEach(p -> map.put(p.id, p));
		return map;
	}

	/**
	 * play an action
	 *
	 * @param action the action
	 * @throws ScrabbleException.ForbiddenPlayException
	 */
	public void play(final UUID game, final String action) throws ScrabbleException.ForbiddenPlayException
	{
		final Action a = Action.parse(action);
		REST_TEMPLATE.postForObject(resolve(game, "play"), a, Action.class);
	}

	/**
	 * Play an action.
	 */
	public void play(final UUID game, final oscrabble.data.Action buildAction) throws ScrabbleException.ForbiddenPlayException
	{
		play(game, buildAction.toString());
	}

	/**
	 * Add a player
	 * @param player the player
	 */
	public Player addPlayer(final UUID game, final Player player)
	{
		return REST_TEMPLATE.postForObject(resolve(game, "addPlayer"), player, Player.class);
	}

	/**
	 * Resolve the uri for a game method
	 * @param game
	 * @param method
	 * @return the uri.
	 */
	private URI resolve(final UUID game, final String method)
	{
		return this.uri.resolve(
				(game == null ? "" : ("/" + game))
						+ "/" + method);
	}

	/**
	 * Start the game.
	 */
	public void startGame(final UUID game)
	{
		REST_TEMPLATE.getForObject(resolve(game, "startGame"), Void.class);
	}

	/**
	 * Get the grid.
	 */
	public Grid getGrid(final UUID game) throws ScrabbleException.CommunicationException
	{
		return Grid.fromData(getState(game).getGrid());
	}

	/**
	 * Wait after a turn has finished.  // TODO: timeout
	 * @param turnNumber the turn number (1 based)
	 */
	public void awaitEndOfPlay(final UUID game, final int turnNumber) throws ScrabbleException.CommunicationException, InterruptedException
	{
		while (true)
		{
			final GameState state = getState(game);
			if (state.turnId > turnNumber)
			{
				break;
			}
			Thread.sleep(100);
		}
	}
}
