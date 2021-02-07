package oscrabble.data;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class PlayerSignature
{
	public UUID player;
}
