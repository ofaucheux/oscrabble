package oscrabble.json;

/**
 * Error describing an I/O problem.
 */
public class IOScrabbleError extends Error
{
	/**
	 *
	 * @param message -
	 * @param cause -
	 */
	public IOScrabbleError(final String message, final Throwable cause)
	{
		super(message, cause);
	}
}
