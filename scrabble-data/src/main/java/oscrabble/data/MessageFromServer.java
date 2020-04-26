package oscrabble.data;

import lombok.Data;

import java.util.UUID;

@Data
public class MessageFromServer
{
	public UUID id;
	public String text;
}
