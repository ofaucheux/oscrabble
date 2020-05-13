package oscrabble.data;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;

@Data
@Builder
public class Bag
{
	public ArrayList<Tile> tiles;
}
