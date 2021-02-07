package oscrabble.data;

import lombok.Builder;
import lombok.Data;

/**
 * Score of a possible move.
 */
@Data
@Builder
public class Score
{
	String notation;
	int score;
}