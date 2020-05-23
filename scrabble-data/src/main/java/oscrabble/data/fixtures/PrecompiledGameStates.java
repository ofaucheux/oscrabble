package oscrabble.data.fixtures;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import oscrabble.data.GameState;

public class PrecompiledGameStates
{
	private PrecompiledGameStates()
	{
		//not instantiable
	}

	@SneakyThrows
	public static GameState game1()
	{
		return new ObjectMapper().readValue(
				PrecompiledGameStates.class.getResourceAsStream("game_1.json"),
				GameState.class
		);
	}
}
