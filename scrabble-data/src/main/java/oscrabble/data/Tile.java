package oscrabble.data;

import lombok.Data;

/**
 * A tile
 */
@Data
public class Tile
{
	public boolean isJoker;
	public Character c;
	public int points;
}
