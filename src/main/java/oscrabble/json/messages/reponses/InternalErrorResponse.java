package oscrabble.json.messages.reponses;

import oscrabble.json.JsonMessage;

/**
 * Message reporting an internal error
 */
public class InternalErrorResponse extends JsonMessage {
	/**
	 * The error message
	 */
	public String errorMessage;

	public void setErrorMessage(final String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getErrorMessage() {
		return this.errorMessage;
	}
}
