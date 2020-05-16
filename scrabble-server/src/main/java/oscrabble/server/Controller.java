package oscrabble.server;

import org.apache.commons.collections4.map.LinkedMap;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import oscrabble.ScrabbleException;
import oscrabble.data.*;

import java.util.UUID;

@RestController
public class Controller
{
	public static final Logger LOGGER = LoggerFactory.getLogger(Controller.class);
	private static final LinkedMap<UUID, Game> GAMES = new LinkedMap<>();

	/**
	 * This value always leads to the last created game.
	 */
	public static final UUID UUID_ZERO = new UUID(0, 0);

	public Controller()
	{
	}

	@PostMapping(value = "/{game}/getState", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<GameState> getState(final @PathVariable UUID game) throws ScrabbleException
	{
		LOGGER.trace("Called: getState()");
		return ResponseEntity.ok(getGame(game).getGameState());
	}


	/**
	 * Get an already known game.
	 * @param uuid id
	 * @return the game with this id
	 * @throws ScrabbleException if no seach game.
	 */
	private static Game getGame(UUID uuid) throws ScrabbleException
	{
		if (uuid.equals(UUID_ZERO))
		{
			if (GAMES.isEmpty())
			{
				throw new ScrabbleException("No game created");
			}
			uuid = GAMES.lastKey();
		}

		final Game game = GAMES.get(uuid);
		if (game == null)
		{
			throw new ScrabbleException("No game with id " + uuid);
		}
		return game;
	}

	/**
	 * Tiles in the rack, space for a joker.
	 */
	@PostMapping(value = "/{game}/getRack", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Bag> getRack(final @PathVariable UUID game, final @RequestBody GetRackRequest request) throws ScrabbleException
	{
		return ResponseEntity.ok(getGame(game).getPlayer(request.player).rack);
	}

	@PostMapping(value = "/{game}/addPlayer", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Player> addPlayer(final @PathVariable UUID game, @RequestBody String playername)
	{
		try
		{
			final Player p = Player.builder().id(UUID.randomUUID()).name(playername).build();
			final PlayerInformation pi = getGame(game).addPlayer(p);
			return new ResponseEntity<>(pi.toData(), HttpStatus.OK);
		}
		catch (ScrabbleException e)
		{
			return new ResponseEntity(ExceptionUtils.getStackTrace(e), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Play an action. TODO: the player should sign the action
	 *
	 * @param action action to play
	 * @return ok or not ok.
	 */
	@PostMapping(value = "/{game}/playAction", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<PlayActionResponse> play(@PathVariable UUID game, @RequestBody Action action)
	{
		final PlayActionResponse.PlayActionResponseBuilder aBuilder = PlayActionResponse.builder().action(action);
		try
		{
			final Game g = getGame(game);
			g.play(action);
			aBuilder.success(true).message("success");
		}
		catch (ScrabbleException e)
		{
			aBuilder.success(false).message(e.toString());
		}
		return new ResponseEntity<>(aBuilder.build(), HttpStatus.OK);
	}

	@RequestMapping(value = "/newGame", method = {RequestMethod.GET, RequestMethod.POST}, produces = MediaType.APPLICATION_JSON_VALUE)
	public GameState newGame()
	{
		final Game game = new Game(Game.DICTIONARY);
		GAMES.put(game.id, game);
		return game.getGameState();
	}


	@RequestMapping(value = "/{game}/start", method = { RequestMethod.GET, RequestMethod.POST }, produces = MediaType.APPLICATION_JSON_VALUE)
	public void startGame(@PathVariable UUID game) throws ScrabbleException, InterruptedException
	{
		final Game g = getGame(game);
		g.startGame();
	}
}