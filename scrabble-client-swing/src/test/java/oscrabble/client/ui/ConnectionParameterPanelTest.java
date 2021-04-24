package oscrabble.client.ui;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import oscrabble.client.Application;

import javax.swing.*;

class ConnectionParameterPanelTest {

	@SneakyThrows
	@Test
	void testUI() {
		final Application.ConnectionParameters properties = new Application.ConnectionParameters();
		final ConnectionParameterPanel panel = new ConnectionParameterPanel(properties);

		JOptionPane.showMessageDialog(null, panel);
	}
}