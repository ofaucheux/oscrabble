package oscrabble.data;

import lombok.Builder;
import lombok.Data;

/**
 * Score of a possible move.
 */
@Data
@Builder
public class Score {
	String notation;
	int score;

	public static String getWord(final String notation) {
		return notation.substring(notation.indexOf(' ') + 1);
	}

	String getWord() {
		return getWord(this.notation);
	}
}