package oscrabble.client.ui;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import oscrabble.client.Application;
import oscrabble.client.utils.Starter;

import javax.swing.*;

class ConnectionParameterPanelTest {

	@SneakyThrows
	@Test
	void testUI() {
		final Application.ConnectionParameters properties = new Application.ConnectionParameters();
		final ConnectionParameterPanel panel = new ConnectionParameterPanel(properties, new Starter());

		JOptionPane.showMessageDialog(null, panel);
	}
}