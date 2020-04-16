//package oscrabble.json;
//
//import oscrabble.server.Play;
//import oscrabble.server.Server;
//
//import java.util.concurrent.BlockingQueue;
//
//public class GameListener implements Server.GameListener
//{
//	private final PlayerStub stub;
//
//	public JsonGameListener(final PlayerStub stub)
//	{
//		this.stub = stub;
//	}
//
//	@Override
//	public BlockingQueue<JsonMessage> getIncomingEventQueue()
//	{
//		return stub.getIncomingEventQueue();
//	}
//
//	@Override
//	public void onPlayRequired(final Play play)
//	{
//		// TODO
//	}
//}
