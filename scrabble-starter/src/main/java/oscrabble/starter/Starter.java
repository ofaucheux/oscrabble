package oscrabble.starter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.utils.ApplicationLauncher;
import oscrabble.utils.PidFiles;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class Starter {

	private static final Logger LOGGER = LoggerFactory.getLogger(Starter.class);
	private final ApplicationItem[] items;
	private JPanel panel;

	/** Directories of the jar applications */
	private final Collection<Path> applicationDirectories;

	@lombok.SneakyThrows
	Starter() {
		this.items = new ApplicationItem[]{
				new ApplicationItem("Dictionary", "scrabble-dictionary", () -> PidFiles.isDictionaryRunning()),
				new ApplicationItem("Server", "scrabble-server", () -> PidFiles.isServerRunning()),
				new ApplicationItem("Client", "scrabble-client-swing", () -> true) // todo
		};

		final Path currentJarDirectory = new File(
				ApplicationLauncher.class
						.getProtectionDomain()
						.getCodeSource()
						.getLocation()
						.toURI()
		).getParentFile().toPath();

		this.applicationDirectories = Arrays.asList(
				currentJarDirectory,
				currentJarDirectory.resolve("../../scrabble-server/build/libs"),
				currentJarDirectory.resolve("../../scrabble-dictionary/build/libs"),
				currentJarDirectory.resolve("../../../scrabble-server/build/libs"),
				currentJarDirectory.resolve("../../../scrabble-dictionary/build/libs")
		);

		createUI();
	}

	public static void main(String[] args) {
		final Starter starter = new Starter();
		final JFrame frame = new JFrame("Scrabble starter");
		frame.add(starter.panel);
		frame.setVisible(true);
		frame.pack();
		frame.setLocationRelativeTo(null);

		starter.start();
	}

	public void start() {
		for (final ApplicationItem item : this.items) {
			// start the thread
			new Thread(
					() -> startAndWaitDone(item),
					item.nameLabel.getText()
			).start();
		}
	}

	private void startAndWaitDone(final ApplicationItem item) {
		if (item.isStartedFunction.get()) {
			item.setState(State.RUNNING);
			return;
		}

		item.setState(State.STARTING);
		try {
			ApplicationLauncher.findAndStartJarApplication(this.applicationDirectories, item.jarNamePattern, false);
			// wait till the application has started
			do {
				//noinspection BusyWait
				Thread.sleep(500);
			} while (!item.isStartedFunction.get());
			item.setState(State.RUNNING);
		} catch (Throwable e) {
			LOGGER.error("Error starting " + item.nameLabel.getText(), e);
			item.setState(State.ERROR);
		}
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
		STARTING(Color.BLACK, "⌛"),
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
		/** Function informing about the state of the application */
		private final Supplier<Boolean> isStartedFunction;

		public ApplicationItem(String name, String jarNamePrefix, final Supplier<Boolean> isStartedFunction) {
			this.nameLabel = new JLabel(name);
			this.isStartedFunction = isStartedFunction;
			this.stateLabel = new JLabel(" ");
			this.stateLabel.setFont(this.stateLabel.getFont().deriveFont(18f));
			this.jarNamePattern = Pattern.compile(Pattern.quote(jarNamePrefix) + ".*\\.jar");
		}

		public void setState(State state) {
			this.stateLabel.setText(state.symbol);
			this.stateLabel.setForeground(state.color);
		}
	}
}
