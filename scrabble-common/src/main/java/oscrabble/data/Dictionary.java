package oscrabble.data;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Data object for dictionary
 */
@Data
@Builder
public class Dictionary {
	public Set<String> words;
}
