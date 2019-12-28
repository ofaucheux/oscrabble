package oscrabble.server;

import oscrabble.dictionary.Dictionary;

/**
 * Game for test purposes
 */
public class DummyGame extends Game
{

	public DummyGame()
	{
		super(Dictionary.getDictionary(Dictionary.Language.TEST));
	}
}
