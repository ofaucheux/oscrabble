package oscrabble.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import oscrabble.json.messages.reponses.InternalErrorResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public abstract class MessageHandler extends AbstractHandler {

	public static final Logger LOGGER = Logger.getLogger(MessageHandler.class);

	@Override
	public void handle(String target, final Request baseRequest, HttpServletRequest request, final HttpServletResponse response) throws IOException {
		final String posted = IOUtils.toString(baseRequest.getReader());
		final JsonMessage post;
		try {
			post = JsonMessage.parse(posted);
		} catch (JsonProcessingException e) {
			final StringWriter stack = new StringWriter();
			e.printStackTrace(new PrintWriter(stack));

			LOGGER.info(e.getMessage() + " with content:\n" + posted);
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Cannot parse posted message\n\n" + e.toString() + "\n" + stack);
			return;
		}

		JsonMessage responseMessage;
		try {
			responseMessage = treat(post);
		} catch (Throwable e) {
			LOGGER.error("Treatment failed", e);

			final StringWriter stack = new StringWriter();
			e.printStackTrace(new PrintWriter(stack));

			responseMessage = JsonMessage.instantiate(InternalErrorResponse.class, post.getGame(), null /* TODO? */, post.getTo());
			((InternalErrorResponse) responseMessage).setErrorMessage(e.toString() + "\n\n" + stack);
		}
		response.getWriter().write(responseMessage.toJson());
		response.setContentType("application/json");
		response.setStatus(HttpServletResponse.SC_OK);
		baseRequest.setHandled(true);
	}

	/**
	 * Treat an incoming message.
	 *
	 * @param incoming the incoming message
	 * @return the response
	 */
	protected abstract JsonMessage treat(final JsonMessage incoming) throws Exception;
}
