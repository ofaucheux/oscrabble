package oscrabble;

public class ScrabbleException extends Exception
{
	public final ERROR_CODE code;

	public ScrabbleException(final ERROR_CODE code, final Throwable cause, final String details)
	{
		super(code.message + (details == null || details.isEmpty() ? "" : "\n" + details), cause);
		this.code = code;
	}

	public boolean acceptRetry()
	{
		return this.code == ERROR_CODE.WHITE_POSITION_REQUIRED;
	}

	public ScrabbleException(final ERROR_CODE code, final String details)
	{
		this(code, null, details);
	}

	public ScrabbleException(final ERROR_CODE code)
	{
		this(code, null);
	}

	public enum ERROR_CODE
	{
		NOT_IDENTIFIED("caller not identified"),
		PLAYER_IS_OBSERVER("player is an observer"),
		FORBIDDEN("Forbidden"), MISSING_LETTER("Missing letter im Rack"),
		ASSERTION_FAILED("Assertion failed"),
		WHITE_POSITION_REQUIRED("Position of the white tiles required");

		private final String message;

		ERROR_CODE(final String message)
		{
			this.message = message;
		}

	}
}
