package oscrabble.client.ui;

import oscrabble.client.Application;

import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.text.NumberFormat;

/**
 * Panel displaying the values of a {@link oscrabble.client.Application.ConnectionParameters}
 */
public class ConnectionParameterPanel extends JPanel {
	final Application.ConnectionParameters properties;

	public ConnectionParameterPanel(final Application.ConnectionParameters properties) {
		this.properties = properties;
		createUI(properties);
	}

	private static JFormattedTextField createNumberField(Integer value) {
		NumberFormat format = NumberFormat.getInstance();
		format.setGroupingUsed(false);
		NumberFormatter formatter = new NumberFormatter(format);
		formatter.setAllowsInvalid(false);
		final JFormattedTextField field = new JFormattedTextField(formatter);
		field.setValue(value == null ? -1 : value);
		return field;
	}

	private void createUI(final Application.ConnectionParameters properties) {
		setLayout(new GridBagLayout());
		final GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridy = 0;
		gbc.gridx = 0;
		gbc.ipadx = 4;

		final Panel buttonPanel = new Panel(new FlowLayout());
		final ButtonGroup buttonGroup = new ButtonGroup();
		final JRadioButton rbLocal = new JRadioButton("Local");
		buttonGroup.add(rbLocal);
		buttonPanel.add(rbLocal, gbc);
		final JRadioButton rbInternet = new JRadioButton("Internet");
		buttonGroup.add(rbInternet);
		buttonPanel.add(rbInternet, gbc);
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.anchor = GridBagConstraints.WEST;
		add(buttonPanel, gbc);

		gbc.gridy++;
		gbc.gridx = 0;
		gbc.gridwidth = 1;

		add(new JLabel("Server"), gbc);
		gbc.gridx++;
		final JTextField server = new JTextField(properties.getServerName());
		server.setColumns(20);
		add(server, gbc);
		gbc.gridx++;
		add(new JLabel("Port"), gbc);
		gbc.gridx++;
		final JTextField port = createNumberField(properties.getServerPort());
		port.setColumns(6);
		add(port, gbc);

		//
		// Listeners
		//
		final ActionListener updateProperties = e -> {
			properties.setServerName(server.getText());
			final String portText = port.getText().trim();
			properties.setServerPort(Integer.parseInt(portText.isEmpty() ? "-1" : portText));
		};

		rbLocal.addActionListener(a -> {
			properties.setLocalServer(true);
			server.setEnabled(false);
			port.setEnabled(false);
		});

		rbInternet.addActionListener(a -> {
			properties.setLocalServer(false);
			server.setEnabled(true);
			port.setEnabled(true);
		});

		server.addActionListener(updateProperties);
		port.addActionListener(updateProperties);

		//
		// Start values
		//
		if ("localhost".equals(properties.getServerName())) {
			rbLocal.setSelected(true);
		} else {
			rbInternet.setSelected(true);
		}
	}
}
