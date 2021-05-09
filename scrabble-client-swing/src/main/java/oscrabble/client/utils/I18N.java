package oscrabble.client.utils;

import java.text.MessageFormat;
import java.util.ResourceBundle;

public class I18N {
	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(
			"Messages", //NON-NLS
			java.util.Locale.FRANCE,
			new CustomResourceBundleControl("UTF-8") //NON-NLS
		); //NON-NL

	public static String get(String key) {
		return RESOURCE_BUNDLE.getString(key);
	}

	public static String get(final String s, final Object ... arguments) {
		return MessageFormat.format(RESOURCE_BUNDLE.getString(s), arguments);
	}
}
