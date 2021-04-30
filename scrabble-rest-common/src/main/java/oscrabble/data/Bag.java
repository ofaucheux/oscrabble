package oscrabble.data;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class Bag {
	public ArrayList<Tile> tiles;

	public String toString() {
		final StringBuffer sb = new StringBuffer();
		this.tiles.forEach(t -> sb.append(t.c));
		return sb.toString();
	}

	public List<Character> getChars() {
		final ArrayList<Character> characters = new ArrayList<>(this.tiles.size());
		this.tiles.forEach(t -> characters.add(t.c));
		return characters;
	}
}
