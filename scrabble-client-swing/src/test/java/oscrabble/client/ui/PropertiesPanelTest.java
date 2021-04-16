package oscrabble.client.ui;

import lombok.Data;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class PropertiesPanelTest {

	/**
	 * test disabled because requires user interaction.
	 */
	@Disabled
	@Test
	void displayPanel() {
		final PropertyObject properties = new PropertyObject();
		properties.value = 2;

		final ArrayList<PropertiesPanel> panels = new ArrayList<>();
		panels.add(new PropertiesPanel(properties));
		panels.add(new PropertiesPanel(properties));

		Executors.newScheduledThreadPool(8).scheduleAtFixedRate(
				() -> { panels.forEach(p -> p.refreshContent());},
				0,
				2,
				TimeUnit.SECONDS
		);

		final JPanel demoPanel = new JPanel(new GridLayout(0, 2));
		panels.forEach(p -> demoPanel.add(p));
		JOptionPane.showConfirmDialog(null, demoPanel);
		System.out.println(properties);
	}
	@Data
	private static class PropertyObject {

		boolean chosen;
		String text;
		int value;
		LocalDate date;
	}
}