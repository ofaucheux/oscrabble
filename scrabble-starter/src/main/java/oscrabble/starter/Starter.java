package oscrabble.starter;

import oscrabble.utils.ApplicationLauncher;

import javax.swing.*;
import java.awt.*;
import java.util.regex.Pattern;

public class Starter {

	private final ApplicationItem[] items;
	private JPanel panel;

	Starter() {
		this.items = new ApplicationItem[]{
				new ApplicationItem("Dictionary", "scrabble-dictionary"),
				new ApplicationItem("Server", "scrabble-server"),
				new ApplicationItem("Client", "scrabble-client-swing")
		};
		createUI();
	}

	public static void main(String[] args) throws InterruptedException {
		final Starter starter = new Starter();
		final JFrame frame = new JFrame("Scrabble starter");
		frame.add(starter.panel);
		frame.setVisible(true);
		frame.pack();
		frame.setLocationRelativeTo(null);

		starter.start();
	}

	public void start() throws InterruptedException {
		for (final ApplicationItem item : this.items) {
			item.setState(State.STARTING);
			new Thread(
					() -> {ApplicationLauncher.findAndStartJarApplication(null, item.jarNamePattern, false);},
					item.nameLabel.getText()
			).start();
			Thread.sleep(2000);
			item.setState(State.RUNNING);
		}

		boolean done;
		do {
			done = true;
			for (final ApplicationItem item : this.items) {
				if (item.state == State.STARTING) {
					done = false;
					break;
				}
			}
			Thread.sleep(500);
		} while (!done);
	}

	private void createUI() {
		this.panel = new JPanel();
		this.panel.setMinimumSize(new Dimension(300, 200));
		this.panel.setLayout(new GridBagLayout());

		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(4, 4, 4, 4);

		for (final ApplicationItem item : this.items) {
			gbc.gridy++;
			gbc.gridx = 0;
			this.panel.add(item.nameLabel, gbc);
			gbc.gridx++;
			this.panel.add(item.stateLabel, gbc);
		}
	}

	/**
	 *
	 */
	private enum State {
		STARTING(Color.YELLOW, "⌛"),
		RUNNING(Color.GREEN.darker(), "✓"),
		ERROR(Color.RED, "✗");

		private final String symbol;
		private final Color color;

		State(final Color color, final String symbol) {
			this.color = color;
			this.symbol = symbol;
		}
	}

	/**
	 *
	 */
	private static class ApplicationItem {
		private final JLabel stateLabel;
		private final JLabel nameLabel;
		private final Pattern jarNamePattern;
		private State state;

		public ApplicationItem(String name, String jarNamePrefix) {
			this.nameLabel = new JLabel(name);
			this.stateLabel = new JLabel(" ");
			this.stateLabel.setFont(this.stateLabel.getFont().deriveFont(18f));
			this.jarNamePattern = Pattern.compile(Pattern.quote(jarNamePrefix) + ".*\\.jar");
		}

		public void setState(State state) {
			this.stateLabel.setText(state.symbol);
			this.stateLabel.setForeground(state.color);
			this.state = state;
		}
	}
}
