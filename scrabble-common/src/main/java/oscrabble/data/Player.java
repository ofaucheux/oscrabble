package oscrabble.data;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class Player
{
	public UUID id;
	public String name;
	public int score;
	public boolean isRobot;
	public boolean isAttached;
}
