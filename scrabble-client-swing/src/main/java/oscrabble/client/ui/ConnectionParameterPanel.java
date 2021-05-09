package oscrabble.client.ui;

import oscrabble.client.Application;

import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.ResourceBundle;

/**
 * Panel displaying the values of a {@link oscrabble.client.Application.ConnectionParameters}
 */
public class ConnectionParameterPanel extends JPanel {

	public final static ResourceBundle MESSAGES = Application.MESSAGES;

	final Application.ConnectionParameters properties;
	private JTextField server;
	private JTextField port;

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
		final JRadioButton rbLocal = new JRadioButton(MESSAGES.getString("parameter.play.mode.value.local"));
		buttonGroup.add(rbLocal);
		buttonPanel.add(rbLocal, gbc);
		final JRadioButton rbInternet = new JRadioButton(MESSAGES.getString("parameter.play.mode.choice.internet"));
		buttonGroup.add(rbInternet);
		buttonPanel.add(rbInternet, gbc);
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.anchor = GridBagConstraints.WEST;
		add(buttonPanel, gbc);

		gbc.gridy++;
		gbc.gridx = 0;
		gbc.gridwidth = 1;

		add(new JLabel(MESSAGES.getString("parameter.label.servername")), gbc);
		gbc.gridx++;
		this.server = new JTextField(properties.getServerName());
		this.server.setColumns(20);
		add(this.server, gbc);
		gbc.gridx++;
		add(new JLabel(MESSAGES.getString("parameter.labe.serverport")), gbc);
		gbc.gridx++;
		this.port = createNumberField(properties.getServerPort());
		this.port.setColumns(6);
		add(this.port, gbc);

		//
		// Listeners
		//
		final ActionListener updateProperties = e -> {
			properties.setServerName(this.server.getText());
			final String portText = this.port.getText().trim();
			properties.setServerPort(Integer.parseInt(portText.isEmpty() ? "-1" : portText));
		};

		rbLocal.addActionListener(a -> updateComponentStatus(true));

		rbInternet.addActionListener(a -> updateComponentStatus(false));

		this.server.addActionListener(updateProperties);
		this.port.addActionListener(updateProperties);

		//
		// Start values
		//
		final String serverName = properties.getServerName();
		if (
			serverName == null
			|| serverName.trim().isEmpty()
			|| serverName.equalsIgnoreCase("localhost")
		) {
			rbLocal.setSelected(true);
			updateComponentStatus(true);
		} else {
			rbInternet.setSelected(true);
			updateComponentStatus(false);
		}
	}

	/**
	 * @param local is the "local" button selected?
	 */
	private void updateComponentStatus(boolean local) {
		this.properties.setLocalServer(local);
		this.server.setEnabled(!local);
		this.port.setEnabled(!local);
	}
}