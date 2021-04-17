package oscrabble.data;

import org.junit.jupiter.api.Test;

import javax.xml.stream.events.NotationDeclaration;

import static org.junit.jupiter.api.Assertions.*;

class ScoreTest {

	@Test
	void getWord() {
		final Score score = Score.builder().notation("K12 ET").score(5).build();
		assertEquals("ET", score.getWord());
	}
}