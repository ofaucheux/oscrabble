package oscrabble.controller;

import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import oscrabble.ScrabbleException;
import oscrabble.data.*;
import oscrabble.data.Action;
import oscrabble.data.objects.Grid;

import java.net.URI;
import java.util.*;

public class MicroServiceScrabbleServer
{
	/**
	 * Default port of scrabble servers
	 */
	public static final int DEFAULT_PORT = 2511;

	@Autowired
	private static final RestTemplate REST_TEMPLATE = new RestTemplate();
	public static final Logger LOGGER = LoggerFactory.getLogger(MicroServiceScrabbleServer.class);

	private final UriComponentsBuilder uriComponentsBuilder;

	/**
	 * @param host Servername
	 * @param port port
	 */
	public MicroServiceScrabbleServer(final String host, final int port)
	{
		this.uriComponentsBuilder = UriComponentsBuilder.newInstance()
				.scheme("http")
				.host(host)
				.port(port);
	}

	/**
	 * @return server on localhost with default port
	 */
	public static MicroServiceScrabbleServer getLocal()
	{
		return new MicroServiceScrabbleServer("localhost", MicroServiceScrabbleServer.DEFAULT_PORT);
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
		final ResponseEntity<GameState> re = REST_TEMPLATE.postForEntity(resolve(game, "getState"), null, GameState.class);
		if (!re.getStatusCode().is2xxSuccessful())
		{
			throw new ScrabbleException.CommunicationException("Cannot get state: " + re.getStatusCode().getReasonPhrase());
		}
		final GameState gameState = re.getBody();
		//noinspection ConstantConditions
		LOGGER.trace("Get state returns: " + gameState.toString());
		return gameState;
	}

	/**
	 * Compute the score of actions. No check against dictionary occurs.
	 * @param game
	 * @param notations
	 * @return
	 * @throws ScrabbleException.CommunicationException
	 */
	public Collection<Score> getScores(final UUID game, final Collection<String> notations) throws ScrabbleException.CommunicationException
	{
		//noinspection ToArrayCallWithZeroLengthArrayArgument
		final ResponseEntity<Score[]> re = REST_TEMPLATE.postForEntity(
				resolve(game, "getScores"),
				notations.toArray(new String[notations.size()]),
				Score[].class);
		if (!re.getStatusCode().is2xxSuccessful())
		{
			throw new ScrabbleException.CommunicationException("Cannot get scores of " + notations);
		}
		//noinspection ConstantConditions
		return Arrays.asList(re.getBody());
	}

	/**
	 * Get the bag of a player.
	 *
	 * @param game
	 * @param player
	 * @return
	 */
	public Bag getRack(final UUID game, final UUID player /* todo: secret */)
	{
		final GetRackRequest request = GetRackRequest.builder().player(player).build();
		final Bag bag = REST_TEMPLATE.postForObject(resolve(game, "getRack"), request, Bag.class);
		return bag;
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
//
//	/**
//	 * play an action
//	 *
//	 * @param action the action
//	 * @throws ScrabbleException.ForbiddenPlayException
//	 */
//	public void play(final UUID game, final String action) throws ScrabbleException.ForbiddenPlayException
//	{
//		final Action a = Action.parse(action);
//		REST_TEMPLATE.postForObject(resolve(game, "play"), a, Action.class);
//	}

	/**
	 * Play an action.
	 */
	public PlayActionResponse play(final UUID game, final Action buildAction) throws ScrabbleException
	{
		final PlayActionResponse response = REST_TEMPLATE.postForObject(resolve(game, "playAction"), buildAction, PlayActionResponse.class);
		return response;
	}

	/**
	 * Add a player
	 * @param name of the player
	 */
	public UUID addPlayer(final UUID game, final String name)
	{
		//noinspection ConstantConditions
		return REST_TEMPLATE.postForObject(resolve(game, "addPlayer"), name, Player.class).id;
	}

	/**
	 * Resolve the uri for a game method
	 * @param game
	 * @param method
	 * @return the uri.
	 */
	private synchronized URI resolve(final UUID game, final String method)
	{
		return this.uriComponentsBuilder
				.replacePath(game == null ? null : game.toString())
				.pathSegment(method)
				.build()
				.toUri();
	}

	/**
	 * Start the game.
	 */
	public void startGame(final UUID game)
	{
		REST_TEMPLATE.postForObject(resolve(game, "start"), null, Void.class);
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
