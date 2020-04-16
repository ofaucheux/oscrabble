package oscrabble.json.messages.reponses;

import oscrabble.json.JsonMessage;
import oscrabble.json.messages.ResponseMessage;
import oscrabble.json.messages.requests.AddPlayer;

/**
 * Response of server to the client after an {@link AddPlayer}
 */
public class AddPlayerResponse extends ResponseMessage
{
	/**
	 * Name of the new player
	 */
	private String playerName;

	/**
	 * Key of the new player
	 */
	private String playerKey;

	public String getPlayerKey()
	{
		return playerKey;
	}

	public void setPlayerKey(final String playerKey)
	{
		this.playerKey = playerKey;
	}

	public String getPlayerName()
	{
		return playerName;
	}

	public void setPlayerName(final String playerName)
	{
		this.playerName = playerName;
	}
}
