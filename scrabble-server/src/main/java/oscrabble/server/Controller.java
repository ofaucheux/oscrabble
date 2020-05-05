package oscrabble.server;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import oscrabble.ScrabbleException;
import oscrabble.data.GameState;
import oscrabble.data.Player;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@org.springframework.stereotype.Controller
public class Controller
{
	public static final Logger LOGGER = LoggerFactory.getLogger(Controller.class);
	final Game game;

	public Controller(final Game game)
	{
		this.game = game;
	}

	@GetMapping(value = "/getState", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<GameState> getState()
	{
		return ResponseEntity.ok(this.game.getGameState());
	}

	@GetMapping(value = "/addPlayer", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> addPlayer(@RequestBody Player player)
	{
		try
		{
			this.game.addPlayer(player);
			return ResponseEntity.ok().build();
		}
		catch (ScrabbleException e)
		{
			return new ResponseEntity(ExceptionUtils.getStackTrace(e), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping(value = "/startGame", produces = MediaType.APPLICATION_JSON_VALUE)
	public void startGame()
	{
		final Runnable c = () -> this.game.play();
		final Future<?> future = Executors.newSingleThreadExecutor().submit(c);
//		try
//		{
//			future.get();
//		}
//		catch (InterruptedException | ExecutionException e)
//		{
//			LOGGER.info("error", e);
//		}

	}
}