package oscrabble.dictionary;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.comparators.ComparatorChain;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import oscrabble.Tile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

public class Dictionary implements Tile.Generator
{

	private static final Logger LOGGER = Logger.getLogger(Dictionary.class);
	private static final CSVFormat letterFileFormat = CSVFormat.newFormat(',')
			.withFirstRecordAsHeader();

	/** Already loaded dictionaries */
	public static final HashMap<Language, Dictionary> LOADED_DICTIONARIES = new HashMap<>();

	private final String name;
	/** Die Wörter: key: Wörter ohne Accent, großgeschrieben. Values: die selben keingeschrieben und mit accent */
	private final MultiValuedMap<String, String> words;
	private final Map<Character, Letter> letters;
	private final Pattern stripAccentPattern;
	private int numberBlanks;

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


			this.words = new HashSetValuedHashMap<>();

			try (final BufferedReader reader = getResourceAsReader(namePrefix + "word_list.txt"))
			{
				assert reader != null;

				String line;
				while ((line = reader.readLine()) != null)
				{
					this.words.put(toUpperCase(line), line);
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
					final LinkedHashSet<String> admissible = new LinkedHashSet<>();
					final HashSet<String> admissibleUppercase = new HashSet<>();
					String line;
					while ((line = reader.readLine()) != null)
					{
						if (!line.isEmpty())
						{
							admissible.add(line);
							admissibleUppercase.add(toUpperCase(line));
						}
					}

					final SortedSet<String> sizeMatchingWords = sizeSortedWords.subSet(
							StringUtils.repeat('A', wordLength),
							StringUtils.repeat('A', wordLength + 1)
					);
					for (final String word : sizeMatchingWords)
					{
						if (!admissibleUppercase.contains(toUpperCase(word)))
						{
							LOGGER.debug("Remove not accepted word " + word);
							this.words.remove(word);
						}
					}

					for (final String word : admissible)
					{
						final String upperCase = toUpperCase(word);
						if (!this.words.containsKey(upperCase))
						{
							LOGGER.trace("Add admissible word " + word);
							this.words.put(upperCase, word);
						}
					}
				}

				this.md5 = DigestUtils.md5Hex(this.words.toString());
			}

			this.letters = new LinkedHashMap<>();
			try (InputStream is = Dictionary.class.getResourceAsStream(namePrefix + "tiles.csv"))
			{
				for (final CSVRecord record : new CSVParser(new InputStreamReader(is), letterFileFormat).getRecords())
				{
					final Letter letter = new Letter();
					final String character = record.get("character");
					if ("blank".equals(character.toLowerCase()))
					{
						this.numberBlanks = Integer.parseInt(record.get("prevalence"));
					}
					else
					{
						if (character.length() != 1)
						{
							throw new AssertionError("False character: " + character);
						}
						letter.c = character.charAt(0);
						letter.prevalence = Integer.parseInt(record.get("prevalence"));
						letter.points = Integer.parseInt(record.get("points"));
						this.letters.put(letter.c, letter);
					}
				}
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

	/**
	 * Liefert die Definitionen eines Wortes.
	 * @return die gefundenen Definitionen, {@code null} wenn keine gefunden.
	 */
	public Iterable<String> getDescriptions(final String word) throws DictionaryException
	{
		if (this.metainformationProvider != null)
		{
			return this.metainformationProvider.getDefinitions(word);
		}
		else
		{
			return null;
		}
	}


	public Collection<Letter> getLetters()
	{
		return this.letters.values();
	}

	public String getName()
	{
		return this.name;
	}

	public void markAsIllegal(final String word)
	{
		// TODO
	}

	@Override
	public Tile generateStone(final Character c)
	{
		final Tile tile;
		if (c == null)
		{
			tile = new Tile(null, 0);
		}
		else
		{
			if (!Character.isUpperCase(c))
			{
				throw new AssertionError("Character must be uppercase: " + c);
			}

			final Letter letter = this.letters.get(c);
			tile = new Tile(letter.c, letter.points);
		}
		return tile;
	}

	/**
	 * @param word Ein Wort, großgeschrieben, z.B. {@code CHANTE}
	 * @return die Wörter, die dazu geführt haben, z.B. {@code chante, chanté}.
	 */
	public Collection<String> getMutations(final String word)
	{
		return Collections.unmodifiableCollection(this.words.get(word));
	}

	public int getNumberBlanks()
	{
		return this.numberBlanks;
	}

	public static class Letter
	{
		public char c;
		public int prevalence;
		int points;
	}


	public enum Language
	{
		FRENCH("french"),
		GERMAN("german"),
		TEST("test")
		;
		final String directoryName;

		Language(final String directoryName)
		{
			this.directoryName = directoryName;
		}
	}

}
