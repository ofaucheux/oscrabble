package oscrabble.data;

import lombok.Data;

@Data
public class Square
{
	public int x;
	public int y;
	public String coordinates;
	public Character tile;
}
