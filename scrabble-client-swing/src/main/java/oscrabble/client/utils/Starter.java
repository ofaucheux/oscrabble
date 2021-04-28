package oscrabble.client.utils;

import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.utils.ApplicationLauncher;
import oscrabble.utils.PidFiles;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class Starter {

	private static final Logger LOGGER = LoggerFactory.getLogger(Starter.class);
	private final ApplicationItem[] items;
	private final HashSet<Process> startedProcesses = new HashSet<>();
	/** Directories of the jar applications */
	private final Collection<Path> applicationDirectories;
	private JPanel panel;

	@lombok.SneakyThrows
	public Starter() {
		this.items = new ApplicationItem[]{
				new ApplicationItem("Dictionary", "scrabble-dictionary", () -> PidFiles.isDictionaryRunning()),
				new ApplicationItem("Server", "scrabble-server", () -> PidFiles.isServerRunning()),
		};

		Path currentJarDirectory = null;
		try {
			currentJarDirectory = new File(
					ApplicationLauncher.class
							.getProtectionDomain()
							.getCodeSource()
							.getLocation()
							.toURI()
			).getParentFile().toPath();
		} catch (IllegalArgumentException e) {
			URI uri = Starter.class.getProtectionDomain().getCodeSource().getLocation()
					.toURI();
			String path = uri.toString().replaceFirst("!.*", "");
			path = path.replaceFirst("^jar:file:[/\\\\]", "");
			LOGGER.info("Current path: " + path);
			currentJarDirectory = new File(path).toPath().getParent();
		}
		LOGGER.info("Current jar directory: " + currentJarDirectory);

		this.applicationDirectories = Arrays.asList(
				currentJarDirectory.normalize(),
				currentJarDirectory.resolve("../../scrabble-server/build/libs").normalize(),
				currentJarDirectory.resolve("../../scrabble-dictionary/build/libs").normalize(),
				currentJarDirectory.resolve("../../../scrabble-server/build/libs").normalize(),
				currentJarDirectory.resolve("../../../scrabble-dictionary/build/libs").normalize()
		);

		Runtime.getRuntime().addShutdownHook(new Thread(() -> killStartedProcesses()));
		createUI();
	}

	@SneakyThrows
	public static void main(String[] args) {
		final Starter starter = new Starter();
		starter.startApplications(false);
		JOptionPane.showMessageDialog(null, starter.panel);
	}

	private void killStartedProcesses() {
		this.startedProcesses.forEach(process -> process.destroy());
	}

	public void startApplications(boolean wait) throws InterruptedException {
		final ArrayList<Thread> threads = new ArrayList<>();
		for (final ApplicationItem item : this.items) {
			// start the thread
			final Thread th = new Thread(
					() -> startAndWaitDone(item),
					item.nameLabel.getText()
			);
			threads.add(th);
			th.start();
		}
		if (wait) {
			for (final Thread thread : threads) {
				thread.join();
			}
		}
	}

	private void startAndWaitDone(final ApplicationItem item) {
		if (item.isStartedFunction.get()) {
			item.setState(State.RUNNING);
			return;
		}

		item.setState(State.STARTING);
		try {
			final File jarFile = new ApplicationLauncher().findJarFile(this.applicationDirectories, item.jarNamePattern);
			final Process startedApplication = ApplicationLauncher.startJarApplication(jarFile);
			this.startedProcesses.add(startedApplication);
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
//		this.panel.setMinimumSize(new Dimension(300, 200));
		this.panel.setLayout(new FlowLayout());

		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(4, 4, 4, 4);

		for (final ApplicationItem item : this.items) {
			this.panel.add(item.nameLabel, gbc);
			gbc.gridx++;
			this.panel.add(item.stateLabel, gbc);
			gbc.gridx++;
		}
	}

	public JPanel getPanel() {
		return this.panel;
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
