package oscrabble.json;

/**
 * Message to transmit to a client its key.
 */
public class SetPlayerKey extends JsonMessage
{
	/**
	 * Key
	 */
	private String key;

	public String getKey()
	{
		return key;
	}

	public void setKey(final String key)
	{
		this.key = key;
	}
}
