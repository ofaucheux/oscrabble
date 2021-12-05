package oscrabble.client.ui;

import oscrabble.client.Application;
import oscrabble.client.utils.I18N;

import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.text.NumberFormat;

/**
 * Panel displaying the values of a {@link oscrabble.client.Application.ConnectionParameters}
 */
public class ConnectionParameterPanel extends JPanel
{

	final Application.ConnectionParameters properties;
	private JTextField server;
	private JTextField port;

	public ConnectionParameterPanel(Application.ConnectionParameters properties) {
		this.properties = properties;

		setLayout(new GridLayout(1, 0));

		final ButtonGroup buttonGroup = new ButtonGroup();
		final JRadioButton rbLocal = new JRadioButton(I18N.get("game.mode.local"));
		rbLocal.setVerticalAlignment(SwingConstants.TOP);
		rbLocal.setHorizontalAlignment(SwingConstants.LEFT);
		buttonGroup.add(rbLocal);
		add(rbLocal);

		final JPanel networkPanel = new JPanel();
		networkPanel.setLayout(new BoxLayout(networkPanel, BoxLayout.Y_AXIS));
		final JRadioButton rbNetwork = new JRadioButton(I18N.get("play.mode.network"));
		rbNetwork.setHorizontalAlignment(SwingConstants.LEFT);
		rbNetwork.setAlignmentX(Component.LEFT_ALIGNMENT);
		buttonGroup.add(rbNetwork);
		networkPanel.add(rbNetwork);
		final JPanel networkParameterPanel = generateNetworkParameterPanel();
		networkParameterPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		networkPanel.add(networkParameterPanel);
		add(networkPanel);

		//
		// Listeners
		//
		final ActionListener updateProperties = e -> {
			this.properties.setServerName(this.server.getText());
			final String portText = this.port.getText().trim();
			this.properties.setServerPort(Integer.parseInt(portText.isEmpty() ? "-1" : portText));
		};

		rbLocal.addActionListener(a -> updateComponentStatus(true));

		rbNetwork.addActionListener(a -> updateComponentStatus(false));

		this.server.addActionListener(updateProperties);
		this.port.addActionListener(updateProperties);

		//
		// Start values
		//
		final String serverName = this.properties.getServerName();
		if (
				serverName == null
						|| serverName.trim().isEmpty()
						|| serverName.equalsIgnoreCase("localhost") //NON-NLS
		) {
			rbLocal.setSelected(true);
			updateComponentStatus(true);
		} else {
			rbNetwork.setSelected(true);
			updateComponentStatus(false);
		}
	}

	private static JFormattedTextField generateNumberField(Integer value) {
		NumberFormat format = NumberFormat.getInstance();
		format.setGroupingUsed(false);
		NumberFormatter formatter = new NumberFormatter(format);
		formatter.setAllowsInvalid(false);
		final JFormattedTextField field = new JFormattedTextField(formatter);
		field.setValue(value == null ? -1 : value);
		return field;
	}

	private JPanel generateNetworkParameterPanel()
	{
		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.insets = new Insets(0, 5, 0, 5);
		final JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		gbc.anchor = GridBagConstraints.WEST;
		gbc.gridy++;
		gbc.gridx = 0;

		this.server = new JTextField(this.properties.getServerName());
		this.server.setColumns(20);
		addLine(
				panel,
				gbc,
				new JLabel(I18N.get("server")),
				this.server
		);

		this.port = generateNumberField(this.properties.getServerPort());
		this.port.setColumns(6);
		addLine(
				panel,
				gbc,
				new JLabel(I18N.get("port")),
				this.port
		);

		return panel;
	}

	private void addLine(JPanel panel, GridBagConstraints gbc, Component c1, Component c2) {
		gbc.gridy++;
		gbc.gridx = 0;
		panel.add(c1, gbc);
		gbc.gridx++;
		panel.add(c2, gbc);
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