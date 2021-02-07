package oscrabble.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import oscrabble.ScrabbleException;
import oscrabble.data.*;

import java.util.List;
import java.util.UUID;

@RestController
public class Controller
{
	public static final Logger LOGGER = LoggerFactory.getLogger(Controller.class);

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
	 * @throws ScrabbleException if no search game.
	 */
	private static Game getGame(UUID uuid) throws ScrabbleException
	{
		return Game.getGame(uuid);
	}

	@PostMapping(value = "/{game}/getScores", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<Score>> getScores(final @PathVariable UUID game, @RequestBody List<String> notations)
	{
		LOGGER.trace("Called: getScores() with " + notations.size() + " actions");
		try
		{
			return ResponseEntity.ok(getGame(game).getScores(notations));
		}
		catch (ScrabbleException e)
		{
			LOGGER.error("Error by call of getScores() with actions: " + notations, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Tiles in the rack, space for a joker.
	 */
	@PostMapping(value = "/{game}/getRack", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Bag> getRack(final @PathVariable UUID game, final @RequestBody PlayerSignature signature) throws ScrabbleException
	{
		return ResponseEntity.ok(getGame(game).getPlayer(signature.player).rack);
	}


	/**
	 * Rules.
	 */
	@PostMapping(value = "/{game}/getRules", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ScrabbleRules> getRules(final @PathVariable UUID game) throws ScrabbleException
	{
		return ResponseEntity.ok(getGame(game).getScrabbleRules());
	}


	@PostMapping(value = "/{game}/addPlayer", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Player> addPlayer(final @PathVariable UUID game, @RequestBody String playerName)
	{
		try
		{
			final Player p = Player.builder().id(UUID.randomUUID()).name(playerName).build();
			final PlayerInformation pi = getGame(game).addPlayer(p);
			return new ResponseEntity<>(pi.toData(), HttpStatus.OK);
		}
		catch (ScrabbleException e)
		{
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
		Game g = null;
		boolean retryAccepted = false;

		try
		{
			g = getGame(game);
			retryAccepted = g.isRetryAccepted();
			g.play(action);
			aBuilder.success(true).message("success");
		}
		catch (ScrabbleException | InterruptedException e)
		{
			aBuilder.success(false).message(e.toString());
		}
		aBuilder.gameState(g == null ? null : g.getGameState())
				.retryAccepted(retryAccepted);
		return new ResponseEntity<>(aBuilder.build(), HttpStatus.OK);
	}

	@RequestMapping(value = "/{game}/acknowledgeState", method = {RequestMethod.POST})
	public void acknowledge(@PathVariable UUID game, @RequestBody final PlayerSignature signature) throws ScrabbleException
	{
		getGame(game).acknowledge(signature.player);
	}

	@RequestMapping(value = "/newGame", method = {RequestMethod.GET, RequestMethod.POST}, produces = MediaType.APPLICATION_JSON_VALUE)
	public GameState newGame()
	{
		final Game game = new Game(Game.DICTIONARY);
		return game.getGameState();
	}

	@RequestMapping(value = "/loadFixtures", method = {RequestMethod.GET, RequestMethod.POST}, produces = MediaType.APPLICATION_JSON_VALUE)
	public List<GameState> loadFixtures()
	{
		return Game.loadFixtures();
	}

	@RequestMapping(value = "/{game}/start", method = { RequestMethod.GET, RequestMethod.POST }, produces = MediaType.APPLICATION_JSON_VALUE)
	public void startGame(@PathVariable UUID game) throws ScrabbleException
	{
		final Game g = getGame(game);
		g.startGame();
	}


	@RequestMapping(value = "/{game}/updatePlayer", method = {RequestMethod.POST}, produces = MediaType.APPLICATION_JSON_VALUE)
	public void updatePlayer(@PathVariable UUID game, @RequestBody PlayerUpdateRequest request) throws ScrabbleException
	{
		final Game g = getGame(game);
		g.updatePlayer(request);
	}
}