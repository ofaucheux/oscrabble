package oscrabble.data;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlayActionResponse
{
	public Action action;
	public boolean success;
	public String message;
}
