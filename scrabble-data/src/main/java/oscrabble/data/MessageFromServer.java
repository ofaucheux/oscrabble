package oscrabble.data;

import lombok.Data;

import java.util.UUID;

@Data
public class MessageFromServer
{
	UUID id;
	String text;
}
