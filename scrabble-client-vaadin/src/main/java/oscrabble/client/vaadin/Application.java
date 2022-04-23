package oscrabble.client.vaadin;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.AppShellSettings;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application extends SpringBootServletInitializer implements AppShellConfigurator {
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Override
	public void configurePage(final AppShellSettings settings) {
		settings.addLink("stylesheet", "https://fonts.googleapis.com/css2?family=Nanum+Gothic+Coding&display=swap");
	}
}
