package oscrabble.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Status;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

public abstract class AbstractMicroService {

	@Autowired
	private static final RestTemplate REST_TEMPLATE = new RestTemplate();

	/**
	 * Wait for up.
	 *
	 * @param timeout
	 * @throws IllegalStateException if not reached after the timeout.
	 */
	protected void waitToUpStatus(final long timeout)
	{
		final Logger logger = LoggerFactory.getLogger(this.getClass());
		final long lastTime = System.currentTimeMillis() + timeout;
		final String serviceURL = getUriComponentsBuilder().toUriString();
		do {
			try {
				final String health = REST_TEMPLATE.getForObject(
						getUriComponentsBuilder().pathSegment("actuator", "health").build().toUri(),
						String.class
				);
				assert health != null;
				//noinspection unchecked
				final Map<String, String> values = new ObjectMapper().readValue(health, Map.class);
				logger.info(serviceURL + " server actual status: " + health);
				if (values.get("status").equals(Status.UP.getCode())) {
					return;
				}
			} catch (RestClientException | JsonProcessingException e) {
				logger.info(String.format("Cannot reach %s: %s", serviceURL, e));
			}
			try {
				//noinspection BusyWait
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				throw new Error(e);
			}
		} while (System.currentTimeMillis() < lastTime);

		throw new IllegalStateException("Dictionary service not found or not upstate");
	}

	protected abstract UriComponentsBuilder getUriComponentsBuilder();
}
