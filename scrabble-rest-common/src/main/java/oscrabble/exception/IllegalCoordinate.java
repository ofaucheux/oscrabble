package oscrabble.exception;

/**
 * The coordinate cannot be parsed
 */
public class IllegalCoordinate extends Error {
	public IllegalCoordinate(final String coordinate) {
		super("Cannot parse coordinate: >" + coordinate + "<");
	}
}
