package oscrabble.data;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;


/**
 * A tile
 */
@Data
@Builder
public class Tile
{
	public boolean isJoker;
	/** The character of a joker will be set when it is played */
	public Character c;
	/** The points of an joker are always 0 */
	public int points;
	/** The position of the tile, {@code null} until it has been played */
	public String position;
	/** The turn the tile has been played, {@code null} if no such one */
	public UUID turn;
}
