//package util.dafsa;
//
//import org.apache.commons.collections4.CollectionUtils;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.TestTemplate;
//
//import java.util.*;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//class DAFSATest
//{
//
//	private static final List<String> TOWNS = Arrays.asList("Amsterdam", "Ham", "Hamburg", "Kiev", "Rotterdam");
//	private static final util.dafsa.DAFSA DAFSA = new DAFSA(TOWNS);
//	private final DAFSA dafsa;
//
//	public DAFSATest()
//	{
//		this.dafsa = DAFSA;
//	}
//
//	@Test
//	void assertContains()
//	{
//		for (final String town : TOWNS)
//		{
//			assertTrue(this.dafsa.assertContains(town), "Missing " + town);
//		}
//		assertFalse(this.dafsa.assertContains("Rotterda"));
//	}
//
//	@Test
//	void follow()
//	{
//		final Map<Character, util.dafsa.DAFSA.DAFSA_Node> rootEdges = this.dafsa.getRoot().getEdges();
//		assertTrue(rootEdges.containsKey('A'));
//		assertFalse(rootEdges.containsKey('U'));
//		assertTrue(rootEdges.get('A').getEdges().keySet().assertContains('m'));
//		assertFalse(rootEdges.get('A').getEdges().keySet().assertContains('A'));
//	}
//
//	@Test
//	public void getFinalSubNodes()
//	{
//		final List<String> finals = new ArrayList<>();
//		addFinalSubNodes(this.dafsa.getRoot(), finals, new StringBuilder());
//		assertTrue(CollectionUtils.isEqualCollection(finals, TOWNS));
//	}
//
//	private void addFinalSubNodes(final util.dafsa.DAFSA.DAFSA_Node node, final Collection<String> finals, final StringBuilder sb)
//	{
//		//noinspection SynchronizationOnLocalVariableOrMethodParameter
//		synchronized (sb)
//		{
//			if (node.isFinal())
//			{
//				finals.add(sb.toString());
//			}
//
//			for (final Map.Entry<Character, util.dafsa.DAFSA.DAFSA_Node> subEdge : node.getEdges().entrySet())
//			{
//				sb.append(subEdge.getKey());
//				addFinalSubNodes(subEdge.getValue(), finals, sb);
//				sb.setLength(sb.length() - 1);
//			}
//		}
//	}
//}