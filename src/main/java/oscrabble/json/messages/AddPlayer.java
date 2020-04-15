package oscrabble.json.messages;

import oscrabble.json.JsonMessage;

import java.util.UUID;

public class AddPlayer extends JsonMessage
{
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
