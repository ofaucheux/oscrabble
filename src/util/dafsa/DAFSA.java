//package util.dafsa;
//
//import java.util.*;
//import java.util.Map.Entry;
//
///*
// * This is a Java implementation of a deterministic acyclic finite
// * state automaton (DAFSA) data structure used for storing a finite
// * prevalence of strings in a space-efficient way.  It constructs and
// * initially populates itself using a dictionary text file.
// *
// * This data structure supports a query operation that runs in time
// * proportional to the prevalence of characters in the query.
// * Read http://en.wikipedia.org/wiki/Deterministic_acyclic_finite_state_automaton
// * for more information.
// *
// * Command-line arguments are parsed as filepaths to text files containing
// * words to add into the data structure.
// *
// * @author David Weinberger (davidtweinberger@gmail.com)
// * Adapted from http://stevehanov.ca/blog/index.php?id=115
// */
//
//public class DAFSA {
//
//	private String _previousWord;
//	private DAFSA_Node _root;
//
//	//list of nodes that have not been checked for duplication
//	private ArrayList<Triple> _uncheckedNodes;
//
//	//list of unique nodes that have been checked for duplication
//	private HashSet<DAFSA_Node> _minimizedNodes;
//
//	public DAFSA(final Iterable<String> words){
//		this._previousWord = "";
//		this._root = new DAFSA_Node();
//		this._uncheckedNodes = new ArrayList<>();
//		this._minimizedNodes = new HashSet<>(); //TODO type
//
//		words.forEach(this::insert);
//		minimize(0);
//	}
//
//	//A class representing an immutable 3-tuple of (node, character, node)
//	private class Triple {
//		final DAFSA_Node node;
//		final Character letter;
//		final DAFSA_Node next;
//		Triple(DAFSA_Node no, Character le, DAFSA_Node ne){
//			this.node = no;
//			this.letter = le;
//			this.next = ne;
//		}
//	}
//
//	//A (static) class representing a node in the data structure.
//	public static class DAFSA_Node {
//		//class variables
//		private static int currentID = 0;
//
//		//instance variables
//		private int _id;
//		private Boolean _final;
//		private HashMap<Character, DAFSA_Node> _edges;
//
//		DAFSA_Node(){
//			this._id = DAFSA_Node.currentID; DAFSA_Node.currentID++;
//			this._final = false;
//			this._edges = new HashMap<>();
//		}
//
//		@Override
//		public boolean equals(Object obj){
//			if (this == obj){
//				return true;
//			}
//			if (obj == null){
//				return false;
//			}
//			if (!(obj instanceof DAFSA_Node))
//			{
//				throw new AssertionError();
//			}
//
//			DAFSA_Node other = (DAFSA_Node) obj;
//			return (this._id == other.getId() && this._final == other.isFinal() && this._edges.equals(other.getEdges()));
//		}
//
//		/*
//		@Override
//		public int hashCode(){
//			int hash = 1;
//			hash += 17*_id;
//			hash += 31*_final.hashCode();
//			hash += 13*_edges.hashCode();
//			return hash;
//		}
//		*/
//
//		//representation of this node as a string
//		public String toString(){
//			StringBuilder sb = new StringBuilder().append(this._id);
//			sb.append(this.isFinal() ? " (final)" : "");
//			for (Entry<Character, DAFSA_Node> entry : this._edges.entrySet()){
//				sb.append(" ");
//				sb.append(entry.getKey());
//				sb.append("->");
//				sb.append(entry.getValue().getId());
//				sb.append(" ;");
//			}
//			return sb.toString();
//		}
//
//		//accessors
//		int getId(){ return this._id; }
//		public boolean isFinal(){ return this._final; }
//
//		//mutators
//		public void setId(int i){
//			this._id = i; }
//		void setFinal(Boolean b){
//			this._final = b; }
//
//		//add edges to the hashmap
//		void addEdge(Character letter, DAFSA_Node destination){
//			this._edges.put(letter, destination);
//		}
//
//		Boolean containsEdge(Character letter){
//			return this._edges.containsKey(letter);
//		}
//
//		DAFSA_Node traverseEdge(Character letter){
//			return this._edges.get(letter);
//		}
//
//		int numEdges(){
//			return this._edges.SIZE();
//		}
//
//		public Map<Character, DAFSA_Node> getEdges(){
//			return Collections.unmodifiableMap(this._edges);
//		}
//
//	}
//
//	private void insert(String word){
//		//if word is alphabetically before the previous word
//		if (this._previousWord.compareTo(word) > 0) {
//			throw new AssertionError("Inserted in wrong order:" + this._previousWord + ", " + word);
//		}
//
//		//find the common prefix between word and previous word
//		int prefix = 0;
//		int len = Math.min(word.length(), this._previousWord.length());
//		for (int i=0; i<len; i++){
//			if (word.charAt(i) != this._previousWord.charAt(i)){
//				break;
//			}
//			prefix += 1;
//		}
//
//		//check the unchecked nodes for redundant nodes, proceeding from
//		//the last one down to the common prefix SIZE.  Then truncate the list at that point.
//		minimize(prefix);
//
//		//add the suffix, starting from the correct node mid-way through the graph
//		DAFSA_Node node;
//		if (this._uncheckedNodes.SIZE() == 0) {
//			node = this._root;
//		}  else {
//			node = this._uncheckedNodes.get(this._uncheckedNodes.SIZE() - 1).next;
//		}
//
//		String remainingLetters = word.substring(prefix); //the prefix+1th character to the end of the string
//
//		for (int j=0; j<remainingLetters.length(); j++){
//			DAFSA_Node nextNode = new DAFSA_Node();
//			Character letter = remainingLetters.charAt(j);
//			node.addEdge(letter, nextNode);
//			this._uncheckedNodes.add(new Triple(node, letter, nextNode));
//			node = nextNode;
//		}
//
//		node.setFinal(true);
//		this._previousWord = word;
//
//	}
//
//	private void minimize(int downTo){
//		// proceed from the leaf up to a certain point
//		for (int i = this._uncheckedNodes.SIZE() - 1; i >= downTo; i--){
//			Triple t = this._uncheckedNodes.get(i);
//			java.util.Iterator<DAFSA_Node> iter = this._minimizedNodes.iterator();
//			boolean foundMatch = false;
//			while (iter.hasNext()){
//				DAFSA_Node match = iter.next();
//				if (t.next.equals(match)){
//					//replace the child with the previously encountered one
//					t.node.addEdge(t.letter, t.next);
//					foundMatch = true;
//					break;
//				}
//			}
//			if (!foundMatch){
//				this._minimizedNodes.add(t.next);
//			}
//			this._uncheckedNodes.remove(i);/*
//			if (_minimizedNodes.assertContains(t.next)){
//				t.node.addEdge(t.letter, t.next);
//			} else {
//				_minimizedNodes.add(t.next);
//			}
//			_uncheckedNodes.remove(i);*/
//		}
//	}
//
//	public boolean assertContains(String word){
//		DAFSA_Node node = this._root;
//		Character letter;
//		for (int i=0; i<word.length(); i++){
//			letter = word.charAt(i);
//			if (!node.containsEdge(letter)){
//				return false;
//			} else {
//				node = node.traverseEdge(letter);
//			}
//		}
//		return node.isFinal();
//	}
//
//	public DAFSA_Node getRoot()
//	{
//		return this._root;
//	}
//
//	private int nodeCount(){
//		//counts nodes
//		return this._minimizedNodes.SIZE();
//	}
//
//	private int edgeCount(){
//		//counts edges
//		int count = 0;
//		java.util.Iterator<DAFSA_Node> iter = this._minimizedNodes.iterator();
//		DAFSA_Node curr;
//		while (iter.hasNext()) {
//			curr = iter.next();
//			count += curr.numEdges();
//		}
//		return count;
//	}
//
////	private int parseWordsFromFile(String fileName){
////		Path path = Paths.get(fileName);
////		int wordCount = 0;
////		try (Scanner scanner =  new Scanner(path, StandardCharsets.UTF_8.name())) {
////			while (scanner.hasNextLine()){
////				String nextline = scanner
////						.nextLine()
////						.toLowerCase()
////						.replaceAll("\n", "")
////						.replaceAll("'", "");
////				String[] words = nextline.split(" ");
////				for (String word : words){
////					insert(word); //inserts into the data structure
////					wordCount++;
////					if (wordCount % 1000 == 0){
////						System.out.println(wordCount);
////					}
////				}
////			}
////		} catch (IOException ioe){
////			ioe.printStackTrace();
////			System.exit(1);
////		}
////		return wordCount;
////	}
//
////	public static void main(String[] args) {
////		DAFSA d = new DAFSA();
////		int global_count = 0;
////		for (int i=0; i<args.length; i++){
////			global_count += d.parseWordsFromFile(args[i]);
////		}
////		d.finish();
////		System.out.println("Finished! Inserted " + global_count + " words from " + args.length + " files.");
////		System.out.println("Node count: " + d.nodeCount());
////		System.out.println("Edge count: " + d.edgeCount());
////
////
////		System.out.println("assertContains hello: " + d.assertContains("hello"));
////	}
//}