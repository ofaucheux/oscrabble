package oscrabble.client.ui;

import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import oscrabble.client.utils.I18N;

import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.*;

/**
 * Panel für die Anzeige und Änderung der Felder eines Objekts.
 * Alle Felder des Java Objects werden angezeigt, die sowohl getter wie setter haben.
 */
public class PropertiesPanel extends JPanel {

	@SuppressWarnings("ConstantConditions")
	private final static Icon trueIcon = new ImageIcon(PropertiesPanel.class.getResource("checkboxTrue.png"));
	@SuppressWarnings("ConstantConditions")
	private final static Icon falseIcon = new ImageIcon(PropertiesPanel.class.getResource("checkboxFalse.png"));

	private final Object propertiesObject;
	private final Map<String, ComponentWrapper<?>> valuesComponents = new HashMap<>();
	private final LinkedHashMap<String, Method> setters = new LinkedHashMap<>();
	private final Map<String, Method> getters = new HashMap<>();

	/**
	 * Crete a panel.
	 *
	 * @param propertiesObject configuration linked with the panel.
	 * @param whitelist selection of fields, if any.
	 */
	public PropertiesPanel(
			final Object propertiesObject,
			final Collection<String> whitelist
	) {
		super();

		this.propertiesObject = propertiesObject;
		final Class<?> propertyClass = this.propertiesObject.getClass();

		for (final Method method : propertyClass.getDeclaredMethods()) {

			final String name = method.getName();
			if (name.startsWith("set")) {
				final String fieldName = StringUtils.uncapitalize(name.substring(3));
				this.setters.put(fieldName, method);
			} else if (name.startsWith("get")) {
				final String fieldName = StringUtils.uncapitalize(name.substring(3));
				this.getters.put(fieldName, method);
			} else if (name.startsWith("is")) {
				final String fieldName = StringUtils.uncapitalize(name.substring(2));
				this.getters.put(fieldName, method);
			}
		}

		if (whitelist != null) {
			this.setters.keySet().retainAll(whitelist);
		}

		setLayout(new GridLayout(0, 2));
		for (final Field field : propertyClass.getDeclaredFields()) {
			final String fieldName = field.getName();
			if (this.setters.containsKey(fieldName) && this.getters.containsKey(fieldName)) {
				final ComponentWrapper<?> componentWrapper = createComponent(field);
				this.valuesComponents.put(fieldName, componentWrapper);
				add(createLabel(field));
				add(componentWrapper.component);
			}
		}

		refreshContent();
	}

	public PropertiesPanel(Object propertiesObject) {
		this(propertiesObject, null);
	}

	private JLabel createLabel(final Field field) {
		final JLabel jLabel = new JLabel(I18N.get(field.getName()));
//		label.setToolTipText(annotation.description().isEmpty() ? labelText : i18n(annotation.description()));

		return jLabel;
	}

	protected ComponentWrapper<?> createComponent(final Field field) {
		final Class<?> type = field.getType();
		final ComponentWrapper<?> paramComponent;

		final String fieldName = field.getName();
		if (String.class.equals(type)) {
			paramComponent = new TextField(
					fieldName,
					field.getType(),
					new JFormattedTextField()
			);
		} else if (type.isEnum()) {
			paramComponent = new ComboBoxField(type, fieldName);
		} else if (boolean.class.equals(type)) {
			paramComponent = new Checkbox(fieldName);
//		} else if (int.class.equals(type) && 			field.getDeclaredAnnotation(LowerBound); /*todo*/) {
//
//			int lower;
//			int upper;
//			lower = annotation.lowerBound();
//			upper = annotation.upperBound();
//			if (lower == upper && upper == -1) {
//				lower = 0;
//				upper = Integer.MAX_VALUE;
//			}
//		} else if (int.class.equals(type)) {
//			if (annotation.isSlide()) {
//				paramComponent = new JSlider(
//						lower,
//						upper,
//						(int) value
//				);
//				((JSlider) paramComponent).setPaintLabels(true);
//				if (upper - lower < 100000) {
//					((JSlider) paramComponent).setMajorTickSpacing((upper - lower) / 2);
//				}
//				((JSlider) paramComponent).addChangeListener(this.listener);
//				listener = evt -> ((JSlider) paramComponent).setValue(((Integer) evt.getNewValue()));
		} else if (int.class.equals(type)) {
			NumberFormat format = NumberFormat.getInstance();
			format.setGroupingUsed(false);
			NumberFormatter formatter = new NumberFormatter(format);
			formatter.setAllowsInvalid(false);
			paramComponent = new TextField(fieldName, Integer.class, new JFormattedTextField(formatter));
		} else if (type == LocalDate.class) {
			paramComponent = new DatePicker(fieldName);
		} else {
			throw new IllegalArgumentException("Cannot treat type " + type);
		}

		return paramComponent;
	}

	protected void setFieldValue(final String fieldName, final Object value) {
		try {
			final Method setter = this.setters.get(fieldName);
			if (setter == null) {
				throw new AssertionError("No setter found for " + fieldName);
			}
			setter.invoke(this.propertiesObject, value);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new AssertionError(e);
		}
	}

	/**
	 * Refresh the content of the panel with the actual values of the properties object.
	 */
	public void refreshContent() {
		for (final String fieldName : this.valuesComponents.keySet()) {
			try {
				final Object value = this.getters.get(fieldName).invoke(this.propertiesObject);
				this.valuesComponents.get(fieldName).setValue(value);
			} catch (IllegalAccessException | InvocationTargetException e) {
				throw new Error("Cannot get component value of field \"" + fieldName + "\"", e);
			}
		}
	}

//	/**
//	 * Translate a text if starting with {@code #}.
//	 *
//	 * @param text text to translate
//	 * @return the translation, or the text itself.
//	 */
//	private static String i18n(final String text) {
//		if (text.startsWith("#")) {
//			return Application.MESSAGES.getString(text.substring(1)); // todo: messages as parameter
//		} else {
//			return text;
//		}
//	}

	protected static abstract class ComponentWrapper<A extends JComponent> {
		A component;

		private Object lastSetValue;

		final synchronized void setValue(Object value) {
			if (
					(value == null && this.lastSetValue == null)
				|| (value != null && value.equals(this.lastSetValue))
			) {
				return;
			}

			setValueIntern(value);
			this.lastSetValue = value;
		}

		protected abstract void setValueIntern(final Object o);
	}

	/**
	 *
	 */
	private class TextField extends ComponentWrapper<JFormattedTextField> {

		private Method valueOfMethod;

		@SneakyThrows
		TextField(final String fieldName, final Class<?> fieldType, JFormattedTextField component) {
			this.component = component;
			try {
				this.valueOfMethod = fieldType.getMethod("valueOf", String.class);
			} catch (NoSuchMethodException e) {
				this.valueOfMethod = fieldType.getMethod("valueOf", Object.class);
			}
			this.component.addFocusListener(new FocusAdapter() {
				@SneakyThrows
				@Override
				public void focusLost(final FocusEvent e) {
					final String text = TextField.this.component.getText();
					final Object value = TextField.this.valueOfMethod.invoke(null, text);
					setFieldValue(fieldName, value);
				}
			});
		}

		@Override
		protected void setValueIntern(final Object o) {
			this.component.setText(String.valueOf(o));
		}
	}

	/**
	 *
	 */
	private class ComboBoxField extends ComponentWrapper<JComboBox<Object>> {
		public ComboBoxField(final Class<?> type, final String fieldName) {
			this.component = new JComboBox<>(type.getEnumConstants());
			this.component.addItemListener(
					e -> {
						if (e.getStateChange() == ItemEvent.SELECTED) {
							setFieldValue(fieldName, this.component.getSelectedItem());
						}
					}
			);
		}

		@Override
		protected void setValueIntern(final Object o) {
			this.component.setSelectedItem(o);
		}
	}

	private class Checkbox extends ComponentWrapper<JCheckBox> {
		public Checkbox(final String fieldName) {
			this.component = new JCheckBox((String) null, false);
			this.component.setIcon(falseIcon);
			this.component.setSelectedIcon(trueIcon);
			this.component.addActionListener(e -> setFieldValue(fieldName, this.component.isSelected()));
		}

		@Override
		protected void setValueIntern(final Object o) {
			this.component.setSelected(((boolean) o));
		}
	}

	@SuppressWarnings("unused") // could be used in the future
	private class Spinner extends ComponentWrapper<JSpinner> {
		public Spinner(final String fieldName) {
			this.component = new JSpinner(new SpinnerNumberModel());
			this.component.addChangeListener(e -> setFieldValue(fieldName, this.component.getValue()));
		}

		@Override
		protected void setValueIntern(final Object o) {
			this.component.setValue(o);
		}
	}

	private class DatePicker extends ComponentWrapper<com.github.lgooddatepicker.components.DatePicker> {
		public DatePicker(final String fieldName) {
			this.component = new com.github.lgooddatepicker.components.DatePicker();
			this.component.addDateChangeListener(e -> setFieldValue(fieldName, this.component.getDate()));
		}

		@Override
		protected void setValueIntern(final Object o) {
			this.component.setDate((LocalDate) o);
		}
	}
}