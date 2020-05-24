package oscrabble.client;

import java.awt.*;

public class SingleComponentFocusTransversalPolicy extends FocusTraversalPolicy
{
	final Component component;

	public SingleComponentFocusTransversalPolicy(final Component component)
	{
		this.component = component;
	}

	@Override
	public Component getComponentAfter(final Container aContainer, final Component aComponent)
	{
		return this.component;
	}

	@Override
	public Component getComponentBefore(final Container aContainer, final Component aComponent)
	{
		return this.component;
	}

	@Override
	public Component getFirstComponent(final Container aContainer)
	{
		return this.component;
	}

	@Override
	public Component getLastComponent(final Container aContainer)
	{
		return this.component;
	}

	@Override
	public Component getDefaultComponent(final Container aContainer)
	{
		return this.component;
	}
}
