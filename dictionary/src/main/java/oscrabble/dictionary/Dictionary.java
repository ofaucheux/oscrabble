package oscrabble.dictionary;

import lombok.Data;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.comparators.ComparatorChain;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

public class Dictionary
{

	private static final Logger LOGGER = LoggerFactory.getLogger(Dictionary.class);


	/** Already loaded dictionaries */
	public static final HashMap<Language, Dictionary> LOADED_DICTIONARIES = new HashMap<>();

	private final String name;

	final HashMap<String, UpperCaseWord> words = new HashMap<>();

	private final Pattern stripAccentPattern;

	public String md5;
	private WordMetainformationProvider metainformationProvider;

	public static Dictionary getDictionary(final Language language)
	{
		Dictionary dictionary = LOADED_DICTIONARIES.get(language);
		if (dictionary ==null)
		{
			dictionary = new Dictionary(language.directoryName);
			LOADED_DICTIONARIES.put(language, dictionary);
		}
		return dictionary;
	}

	private Dictionary(final String name)
	{
		LOGGER.info("Create dictionary " + name);
		this.name = name;

		Properties properties;
		try
		{
			final String namePrefix = name + "/";

			properties = new Properties();
			try (InputStream is = Dictionary.class.getResourceAsStream(namePrefix + name + ".properties"))
			{
				if (is == null)
				{
					throw new AssertionError("Dictionary not found: " + name);
				}
				properties.load(is);
			}

			final String accents = properties.getProperty("acceptedAccents");

			final Set<Character> conserve = new HashSet<>();
			if (accents != null)
			{
				for (final char c : accents.toCharArray())
				{
					conserve.add(c);
				}
			}
			final StringBuilder regex = new StringBuilder("\\p{InCombiningDiacriticalMarks}+");
			if (!conserve.isEmpty())
			{
				regex.insert(0, "[");
				regex.append("&&[^");
				for (final Character c : conserve)
				{
					regex.append(c);
				}
				regex.append("]]");
			}
			this.stripAccentPattern = Pattern.compile(regex.toString());


			String wordLists = properties.getProperty("word.list.files");
			if (wordLists == null)
			{
				wordLists = "word_list.txt";
			}
			for (final String wordList : wordLists.split(";"))
			{
				try (final BufferedReader reader = getResourceAsReader(namePrefix + wordList))
				{
					assert reader != null;

					String line;
					while ((line = reader.readLine()) != null)
					{
						final String uc = toUpperCase(line);
						this.words.computeIfAbsent(uc, s -> new UpperCaseWord(uc)).mutations.add(line);
					}
				}
			}

			final ComparatorChain<String> sizeComparator = new ComparatorChain<>();
			sizeComparator.addComparator((o1,o2)-> o1.length() - o2.length());
			sizeComparator.addComparator(String::compareTo);
			final TreeSet<String> sizeSortedWords = new TreeSet<>(sizeComparator);
			sizeSortedWords.addAll(this.words.keySet());

			for (int wordLength = 2; wordLength < 15; wordLength++)
			{
				try (final BufferedReader reader = getResourceAsReader(namePrefix + "admissible_" + wordLength + "_chars.txt"))
				{
					if (reader == null)
					{
						continue;
					}
					LOGGER.debug("Read Admissible for " + wordLength + " characters");
					final List<String> admissibleWords = IOUtils.readLines(reader);

					final Set<String> refusedWords = new HashSet<>(sizeSortedWords.subSet(
							StringUtils.repeat('A', wordLength),
							StringUtils.repeat('A', wordLength + 1)
					));
					refusedWords.removeAll(admissibleWords);

					for (final String refused : refusedWords)
					{
						final UpperCaseWord uc;
						if ((uc = this.words.get(toUpperCase(refused))) != null)
						{
							uc.mutations.remove(refused);
							if (uc.mutations.isEmpty())
							{
								this.words.remove(uc.uppercase);
							}
						}
					}
				}

				this.md5 = DigestUtils.md5Hex(this.words.toString());
			}

		}
		catch (IOException e)
		{
			throw new IOError(e);
		}

		final String provider = properties.getProperty("metainformation.provider");
		if (provider != null)
		{
			this.metainformationProvider = new UnMotDotNet();
//			((Wikitionary) this.metainformationProvider).setHtmlWidth(200);
		}
	}

	public WordMetainformationProvider getMetainformationProvider()
	{
		return this.metainformationProvider;
	}

	/**
	 * Create a reader one a resource file. The {@code #readLine()} function will strip the comments and trim the line before returning it.
	 *
	 * @param resourceName name of the resource
	 * @return the reader, or {@code null} if no such resource
	 */
	private BufferedReader getResourceAsReader(final String resourceName)
	{
		final InputStream is = Dictionary.class.getResourceAsStream(resourceName);
		if (is == null)
		{
			return null;
		}

		//noinspection UnnecessaryLocalVariable
		final BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
		{
			@Override
			public String readLine() throws IOException
			{
				String line = super.readLine();
				if (line != null)
				{
					final int comment = line.indexOf('#');
					if (comment != -1)
					{
						line = line.substring(0, comment);
					}
					line = line.trim();
				}
				return line;
			}
		};

		return reader;
	}

	/* from StringUtils, modified */
	String stripAccents(String input) {
		if (input == null)
		{
			return null;
		} else {
			String decomposed = Normalizer.normalize(input, Normalizer.Form.NFD);
			decomposed = this.stripAccentPattern.matcher(decomposed).replaceAll("");
			return Normalizer.normalize(decomposed, Normalizer.Form.NFC);
		}
	}

	/**
	 * @return Das Wort ohne Azenkt und großgeschrieben
	 */
	String toUpperCase(final String word)
	{
		return stripAccents(word.toUpperCase());
	}

	public Set<String> getMutations()
	{
		return Collections.unmodifiableSet(this.words.keySet());
	}

	public boolean containUpperCaseWord(final String word)
	{
		final boolean contains = this.getMutations().contains(word);
		LOGGER.trace("is contained " + word + ": " + contains);
		return contains;
	}

	public String getName()
	{
		return this.name;
	}

	public void markAsIllegal(final String word)
	{
		// TODO
	}


	/**
	 * @param word Ein Wort, großgeschrieben, z.B. {@code CHANTE}
	 * @return die Wörter, die dazu geführt haben, z.B. {@code chante, chanté}.
	 */
	public Collection<Mutation> getMutations(final String word) throws DictionaryException
	{
		final WordMetainformationProvider mip = getMetainformationProvider();
		final Set<Mutation> mutations = new HashSet<>();
		for (final String mutation : this.words.get(word).mutations)
		{
			final Mutation m = new Mutation();
			m.word = mutation;
			m.definitions = mip != null ? mip.getDefinitions(mutation) : null;
			mutations.add(m);
		}
		return mutations;
	}

	@Data
	public static class Mutation
	{
		String word;
		Iterable<String> definitions;
	}

	/**
	 * A word and its mutations
	 */
	@Data
	static class UpperCaseWord
	{
		public final String uppercase;
		public final HashSet<String> mutations = new HashSet<>();

		UpperCaseWord(final String uppercase)
		{
			this.uppercase = uppercase;
		}
	}
}
