package oscrabble.data;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DictionaryEntry {
	public String word;
	public List<String> definitions;
}