package oscrabble.data;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import org.springframework.lang.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * Interface for plays a player can play.
 */
@Data
@Builder
public class Action {
	/**
	 * Notation for "pass the turn"
	 */
	public static String PASS_TURN_NOTATION = "-";

	/**
	 * ID of the turn the action is for - none for test purposes
	 */
	public UUID turnId;

	/**
	 * ID of the player
	 */
	public UUID player;

	/**
	 * Die Standardnotation des Spielschritt z.B. {@code B4 WAGEN}. Blanks werden als Kleinbuchstaben dargestellt.
	 */
	public String notation;

	/**
	 * Score of this action, to be filled by the server.
	 */
	@Nullable
	public Integer score;

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final Action action = (Action) o;
		return Objects.equals(this.notation, action.notation);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.notation);
	}

	/**
	 * @return the score, eventually 0
	 */
	@NonNull
	public int getScore() {
		return this.score == null ? 0 : this.score;
	}
}
