package oscrabble.data;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;

@Data
@Builder
public class Bag
{
	public ArrayList<Tile> tiles;

	public String toString()
	{
		final StringBuffer sb = new StringBuffer();
		tiles.forEach(t -> sb.append(t.c));
		return sb.toString();
	}
}
