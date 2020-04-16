package oscrabble.json.messages.reponses;

import oscrabble.json.JsonMessage;
import oscrabble.json.messages.ResponseMessage;
import oscrabble.json.messages.requests.GetName;

/**
 * Answer to a {@link GetName}
 */
public class GetNameResponse extends ResponseMessage
{
	/**
	 * Name of the player
	 */
	private String name;

	public String getName()
	{
		return name;
	}

	public void setName(final String name)
	{
		this.name = name;
	}
}
