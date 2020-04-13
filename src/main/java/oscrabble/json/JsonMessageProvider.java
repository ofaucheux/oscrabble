package oscrabble.json;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Provider for messages
 */
public interface JsonMessageProvider
{
	/**
	 * Write a json message on the queue
	 *
	 * @param to      recipient
	 * @param message published message
	 * @return generated ID of the message
	 */
	UUID publish(UUID from, UUID to, JsonMessage message);

	/**
	 * Read the next json message from the queue for a given recipient.
	 * Blocks
	 * @param to the recipient
	 * @param maxWait max time to wait for
	 * @param maxWaitUnit unit for max time to wait for
	 * @return the read message
	 * @throws TimeoutException if timeout expired
	 */
	JsonMessage readNext(UUID to, int maxWait, TimeUnit maxWaitUnit) throws TimeoutException;

	/**
	 * Read a message
	 * @param messageId the ID of message to read
	 * @return read message
	 * @throws IllegalArgumentException if not such message
	 */
	JsonMessage readMessage(UUID messageId) throws IllegalArgumentException;
}
