package oscrabble.data;

import lombok.Data;

@Data
public class HistoryEntry
{
	public String player;
	public String move;
	public int score;
}
