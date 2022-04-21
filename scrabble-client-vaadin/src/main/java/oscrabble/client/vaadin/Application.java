package oscrabble.client.vaadin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class Application extends SpringBootServletInitializer {
	public static void main(String[] args) {
		new SpringApplicationBuilder(Application.class)
				.headless(false) // required by ScrabbleView.createGridHTML()
				.run(args);
		SpringApplication.run(Application.class, args);
	}
}
