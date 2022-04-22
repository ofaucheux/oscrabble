package oscrabble.client.utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOError;
import java.io.IOException;

public class SwingUtils {

	/**
	 * Print a component as a PNG image. Careful: this function can change the size of the component.
	 *
	 * @param component
	 * @param size In null, the preferred size of the component is used
	 * @return the image of the component as a PNG
	 */
	public static byte[] getImage(Component component, Dimension size) {
		if (size == null) {
			size = component.getPreferredSize();
		}
		component.setVisible(true);
		component.setSize(size);
		layoutComponent(component);

		BufferedImage image = new BufferedImage(
				component.getWidth(),
				component.getHeight(),
				BufferedImage.TYPE_INT_RGB);
		Graphics2D cg = (Graphics2D) image.getGraphics();
		component.print(cg);
		image.flush();
		try {
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			ImageIO.write(image, "png", os);
			os.flush();
			return os.toByteArray();
		} catch (IOException e) {
			throw new IOError(e);
		}
	}

	static void layoutComponent(Component component)
	{
		synchronized (component.getTreeLock())
		{
			component.doLayout();

			if (component instanceof Container)
			{
				for (Component child : ((Container)component).getComponents())
				{
					layoutComponent(child);
				}
			}
		}
	}
}
