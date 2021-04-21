package oscrabble.client.ui;

import oscrabble.client.Application;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class Dialogs {

	/**
	 * Display a dialog to edit the connection parametern
	 * @return
	 * @param connectionParameters
	 */
	public static void displayConnectionParameterDialog(final Application.ConnectionParameters connectionParameters) {
		final JPanel jPanel = new JPanel(new BorderLayout());
		final JCheckBox localServer = new JCheckBox("Local server");
		jPanel.add(localServer, BorderLayout.NORTH);

		connectionParameters.setServerName("localhost");
		connectionParameters.setServerPort(2511);
		jPanel.add(new PropertiesPanel(
				connectionParameters,
				Arrays.asList("serverName", "serverPort")
		));

		JOptionPane.showMessageDialog(
				null,
				jPanel,
				"Server connection",
				JOptionPane.PLAIN_MESSAGE
		);

		connectionParameters.setLocalServer(localServer.isSelected());
	}

}
