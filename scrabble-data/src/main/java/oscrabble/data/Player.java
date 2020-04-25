package oscrabble.data;

import lombok.Data;

import java.util.UUID;

@Data
public class Player
{
	public UUID id;
	public String name;
	public int score;
}
