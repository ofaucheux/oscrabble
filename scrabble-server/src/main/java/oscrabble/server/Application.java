package oscrabble.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

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
		this.applicationContext = SpringApplication.run(Application.class);
	}

	public void stop() {
		this.applicationContext.stop();
	}
}
