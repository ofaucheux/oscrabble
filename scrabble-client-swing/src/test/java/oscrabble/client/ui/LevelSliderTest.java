package oscrabble.client.ui;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.swing.*;

class LevelSliderTest {

	@Test
	@Disabled // disable swing tests
	void testDisplay() {
		final LevelSlider slider = new LevelSlider();
		slider.addChangeListener(
				e -> { System.out.println(slider.getSelectedLevel());}
		);
		JOptionPane.showMessageDialog(null, slider);
	}
}