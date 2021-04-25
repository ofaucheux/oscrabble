package oscrabble.server;

import lombok.SneakyThrows;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import oscrabble.utils.PidFiles;

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

	@SneakyThrows
	@EventListener(ApplicationReadyEvent.class)
	public void writePid() {
		PidFiles.writePid(PidFiles.PID_FILE_SERVER);
	}
}
