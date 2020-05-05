package oscrabble.data;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Player
{
	public String id;
	public String name;
	public int score;
}
