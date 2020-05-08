package oscrabble.server;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import oscrabble.ScrabbleException;
import oscrabble.data.Action;
import oscrabble.data.GameState;
import oscrabble.data.Player;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@org.springframework.stereotype.Controller
public class Controller
{
	public static final Logger LOGGER = LoggerFactory.getLogger(Controller.class);
	private static final HashMap<UUID, Game> GAMES = new HashMap<>();

	public Controller()
	{
	}

	@GetMapping(value = "/{game}/getState", produces = MediaType.APPLICATION_JSON_VALUE)
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
	private static Game getGame(final UUID uuid) throws ScrabbleException
	{
		final Game game = GAMES.get(uuid);
		if (game == null)
		{
			throw new ScrabbleException("No game with id " + uuid);
		}
		return game;
	}

	@GetMapping(value = "/{game}/addPlayer", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<UUID> addPlayer(final @PathVariable UUID game, @RequestBody Player player)
	{
		try
		{
			final PlayerInformation pi = getGame(game).addPlayer(player);
			return new ResponseEntity<>(pi.uuid, HttpStatus.OK);
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

	@GetMapping(value = "/startGame", produces = MediaType.APPLICATION_JSON_VALUE)
	public GameState startGame()
	{
		final Game game = new Game(Game.DICTIONARY);
		final Runnable c = () -> game.play();
		final Future<?> future = Executors.newSingleThreadExecutor().submit(c);
		return game.getGameState();
	}
}