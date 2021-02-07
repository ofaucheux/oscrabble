package oscrabble.data;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class PlayerUpdateRequest
{
	public UUID playerId;
	public String parameter;
	public String newValue;

	public enum Parameter {ATTACHED}
}