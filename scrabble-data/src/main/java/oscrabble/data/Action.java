package oscrabble.data;

import lombok.Data;

import java.util.Objects;
import java.util.UUID;

/**
 * Interface for plays a player can play.
 */
@Data
public class Action
{
	/**
	 * ID of the play
	 */
	public UUID playID;

	/**
	 * ID of the player
	 */
	public UUID player;

	/**
	 * Die Standardnotation des Spielschritt z.B. {@code B4 WAGEN}. Blanks werden als Kleinbuchstaben dargestellt.
	 */
	public String notation;

	@Override
	public boolean equals(final Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final Action action = (Action) o;
		return Objects.equals(this.notation, action.notation);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(this.notation);
	}
}
