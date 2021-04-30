package oscrabble.data;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class PlayerUpdateRequest {
	public UUID playerId;
	public String parameter;
	public String newValue;

	public static PlayerUpdateRequest createAttachRequest(final UUID player, final boolean attach) {
		// todo: without builder
		return PlayerUpdateRequest.builder()
				.playerId(player)
				.parameter(PlayerUpdateRequest.Parameter.ATTACHED.toString())
				.newValue(attach ? Boolean.TRUE.toString() : Boolean.FALSE.toString())
				.build();
	}

	public enum Parameter {ATTACHED,}
}