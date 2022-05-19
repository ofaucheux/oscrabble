package oscrabble.data;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SaveGameResponse {
	public boolean success;
	public String filename;
	public String errorMessage;
}
