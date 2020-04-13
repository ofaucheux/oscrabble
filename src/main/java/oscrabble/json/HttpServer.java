package oscrabble.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.NetworkTrafficServerConnector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpServer extends AbstractHandler
{
	public static final String PARAM_MAX_WAIT = "maxWait";
	public static final String PARAM_MAX_WAIT_UNIT = "maxWaitUnit";

	public static final Pattern NEXT_PATTERN = Pattern.compile("/next/(.*)");
	public static final Logger LOGGER = Logger.getLogger(HttpServer.class);
	/**
	 * Mapping receiver->queue
	 */
	final private Map<UUID, ArrayBlockingQueue<JsonMessage>> queues = new HashMap();

	@Override
	public void handle(final String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException
	{
		try
		{
			final Matcher m;
			String contextPath = baseRequest.getContextPath();
			if (contextPath == null)
			{
				contextPath = "";
			}
			if ((m = NEXT_PATTERN.matcher(contextPath)).matches())
			{
				final String to = m.group(1);
				final ArrayBlockingQueue<JsonMessage> queue = this.queues.get(UUID.fromString(to));
				final long maxWait = Long.parseLong(request.getHeader(PARAM_MAX_WAIT));
				final JsonMessage message = queue.poll(maxWait, TimeUnit.valueOf(request.getHeader(PARAM_MAX_WAIT_UNIT)));
				if (message == null)
				{
					response.setStatus(HttpServletResponse.SC_NO_CONTENT);
					return;
				}
			}
			else
			{
				final String posted = IOUtils.toString(baseRequest.getReader());
				if (posted.isEmpty())
				{
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "POST is empty");
					return;
				}

				final JsonMessage message;
				try
				{
					message = JsonMessage.parse(posted);
				}
				catch (JsonProcessingException e)
				{
					LOGGER.info(e.getMessage() + " with content:\n" + posted);
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Cannot parse posted message");
					return;
				}
				treatMessage(message);
			}
		}
		catch (final Throwable e)
		{
			LOGGER.error("Error occurred", e);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	private void treatMessage(final JsonMessage message)
	{
		throw new Error("Not implemented");
		// TODO
	}

	public static void main(String[] args) throws Exception
	{
		int port = 8080;
		Server server = new Server(port);
		server.setHandler(new HttpServer());

		server.start();
		server.join();
	}
}
