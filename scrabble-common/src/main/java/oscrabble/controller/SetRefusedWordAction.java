package oscrabble.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.util.*;

@SuppressWarnings("HardCodedStringLiteral")
public class SetRefusedWordAction extends oscrabble.controller.Action {

	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

	private final TreeSet<String> refusedWords = new TreeSet<>();

	public SetRefusedWordAction(final Collection<String> words) {
		super(Action.NOT_A_PLAYER_UUID);
		this.refusedWords.addAll(words);
	}

	protected SetRefusedWordAction(final String notation) {
		super(Action.NOT_A_PLAYER_UUID);
		final String[] words;
		try {
			words = JSON_MAPPER.readValue(notation, String[].class);
		} catch (JsonProcessingException e) {
			throw new AssertionError("Input is not parsable: " + notation, e);
		}
		this.refusedWords.addAll(Arrays.asList(words));
	}

	@SneakyThrows
	@Override
	public String getNotation() {
		return "REFUSE:" + JSON_MAPPER.writeValueAsString(this.refusedWords);
	}

	Set<String> getRefusedWords() {
		return Collections.unmodifiableSet(this.refusedWords);
	}
}
