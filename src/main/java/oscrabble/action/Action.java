package oscrabble.action;

/**
 * Interface for plays a player can play.
 */
public interface Action
{

	/**
	 * @return die Standardnotation des Spielschritt z.B. {@code B4 WAGEN}. Blanks werden als Kleinbuchstaben dargestellt.
	 */
	String getNotation();
}
