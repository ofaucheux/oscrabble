package oscrabble.dictionary.metainformationProviders;

import oscrabble.dictionary.DictionaryException;

public interface WordMetainformationProvider
{
	/**
	 * Sucht die Definitionen eines Wortes, in Plain-Text oder HTML, in welchem Fall sie mit <pre>&lt;html&gt;</pre> anfangen.
	 * @param word das gesuchte Wort
	 * @return die Beschreibungen. Empty iterable if no such found
	 */
	Iterable<String> getDefinitions(String word) throws DictionaryException;
}
