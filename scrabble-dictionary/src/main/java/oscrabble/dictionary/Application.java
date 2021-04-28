package oscrabble.dictionary;

import lombok.SneakyThrows;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import oscrabble.utils.PidFiles;
import oscrabble.utils.TempDirectory;

import java.util.HashMap;

/**
 * Microservice starter.
 */
@SpringBootApplication
public class Application {

	private ConfigurableApplicationContext applicationContext;

	public static void main(String[] args) {
		new Application().start();
	}

	public void start() {
		final SpringApplication springApplication = new SpringApplication(Application.class);
		final HashMap<String, Object> properties = new HashMap<>();
		properties.put("logging.file", TempDirectory.getFile("dictionary.log").getPath());
		springApplication.setDefaultProperties(properties);
		this.applicationContext = springApplication.run();
	}

	public void stop() {
		this.applicationContext.stop();
	}

	@SneakyThrows
	@EventListener(ApplicationReadyEvent.class)
	public void writePid() {
		PidFiles.writePid(PidFiles.PID_FILE_DICTIONARY);
	}
}
