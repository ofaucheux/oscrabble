package oscrabble.server;

import org.apache.commons.collections4.map.LinkedMap;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import oscrabble.ScrabbleException;
import oscrabble.data.Action;
import oscrabble.data.GameState;
import oscrabble.data.Player;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
				throw new ScrabbleException("No game cretated");
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
	@PostMapping(value = "/{game}/play", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> play(@PathVariable UUID game, @RequestBody Action action)
	{
		try
		{
			getGame(game).play(action);
			return ResponseEntity.ok().build();
		}
		catch (ScrabbleException e)
		{
			return new ResponseEntity(ExceptionUtils.getStackFrames(e), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping(value = "/newGame", produces = MediaType.APPLICATION_JSON_VALUE)
	public GameState newGame()
	{
		final Game game = new Game(Game.DICTIONARY);
		GAMES.put(game.id, game);
		return game.getGameState();
	}


	@PostMapping(value = "/{game}/start", produces = MediaType.APPLICATION_JSON_VALUE)
	public void startGame(@PathVariable UUID game) throws ScrabbleException, InterruptedException
	{
		final Game g = getGame(game);
		final Runnable c = () -> {
			try
			{
				g.play();
			}
			catch (final Throwable e)
			{
				LOGGER.error("Error playing game", e);
			}
		};
		final Future<?> future = Executors.newSingleThreadExecutor().submit(c);
		Thread.sleep(500);
		if (future.isDone())
		{
			throw new ScrabbleException("Game did not start");
		}
	}
}