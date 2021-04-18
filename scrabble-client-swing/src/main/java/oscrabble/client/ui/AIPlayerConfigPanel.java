package oscrabble.client.ui;

import oscrabble.player.ai.AIPlayer;

import java.lang.reflect.Field;

public class AIPlayerConfigPanel extends PropertiesPanel {
	/**
	 * Crete a panel.
	 *
	 * @param propertiesObject configuration linked with the panel.
	 */
	public AIPlayerConfigPanel(final Object propertiesObject) {
		super(propertiesObject);
	}

	@Override
	protected PropertiesPanel.ComponentWrapper<?> createComponent(final Field field) {

		if (field.getType() == AIPlayer.Level.class) {
			return new LevelSliderWrapper(field.getName());
		}

		return super.createComponent(field);
	}

	/**
	 *
	 */
	private class LevelSliderWrapper extends ComponentWrapper<LevelSlider> {
		LevelSliderWrapper(final String fieldName) {
			this.component = new LevelSlider();
			this.component.addChangeListener( e -> setFieldValue(fieldName, this.component.getSelectedLevel()));
		}

		@Override
		protected void setValueIntern(final Object o) {
			this.component.setValue(((AIPlayer.Level) o).ordinal());
		}
	}
}
