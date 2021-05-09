package oscrabble.dictionary;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
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
}
