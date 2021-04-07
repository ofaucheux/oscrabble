package oscrabble.client.ui;

import oscrabble.data.objects.Grid;

import javax.swing.*;
import java.awt.*;

public class StartWordArrow extends JComponent {

	private Grid.Direction direction;

	public void setDirection(final Grid.Direction direction) {
		this.direction = direction;
	}

	@Override
	public void paintComponent(final Graphics g) {
		if (this.direction == null) {
			return;
		}

		final Graphics2D g2 = (Graphics2D) g.create();

		g2.setColor(Color.BLACK);
		final Polygon p = new Polygon();
		final int h = getHeight();
		final int POLYGONE_SIZE = h / 3;
		p.addPoint(-POLYGONE_SIZE / 2, 0);
		p.addPoint(0, POLYGONE_SIZE / 2);
		p.addPoint(POLYGONE_SIZE / 2, 0);

		switch (this.direction) {
			case VERTICAL:
				g2.translate(h / 2f, 4f);
				break;
			case HORIZONTAL:
				g2.rotate(-Math.PI / 2);
				g2.translate(-h / 2f, 4f);
				break;
			default:
				throw new IllegalStateException("Unexpected value: " + this.direction);
		}
		g2.fillPolygon(p);
	}

}
