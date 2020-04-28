package oscrabble;

public class ScrabbleException extends Exception
{

	public String message;

	public ScrabbleException()
	{
	}

	public ScrabbleException(final String message)
	{
		this.message = message;
	}

	public ScrabbleException(final String message, final Throwable cause)
	{
		this(message + "\nCaused by: " + cause.toString());
	}

	@Override
	public String toString()
	{
		return message;
	}

	/**
	 * Exception reporting the use of an invalid secret.
	 */
	static public class InvalidSecretException extends ScrabbleException
	{
		public InvalidSecretException()
		{
			super("Invalid secret");
		}
	}

	/**
	 * Exception reporting a forbidden action.
	 */
	static public class ForbiddenPlayException extends ScrabbleException
	{
		public ForbiddenPlayException(final String message)
		{
			super(message);
		}
	}

	/**
	 * Exception reporting the use of a function at an time it is not valid to use it.
	 */
	static public class InvalidStateException extends ScrabbleException
	{
		public InvalidStateException(final String message)
		{
			super(message);
		}

	}

	/**
	 * Exception informing about the attempt of a player to play outside of his turn.
	 */
	public static class NotInTurn extends ScrabbleException
	{
		public NotInTurn(final String playerName)
		{
			super("The player " + playerName + " is not in turn");
		}
	}
}
