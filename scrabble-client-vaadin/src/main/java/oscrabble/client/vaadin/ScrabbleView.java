package oscrabble.client.vaadin;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import oscrabble.ScrabbleException;
import oscrabble.data.ScrabbleRules;
import oscrabble.data.Tile;
import oscrabble.data.objects.Grid;
import oscrabble.data.objects.Square;
import oscrabble.dictionary.Language;
import oscrabble.dictionary.ScrabbleRulesFactory;

import java.awt.*;

@Route(value = "")
@PageTitle("Scrabble | By Olivier")
public class ScrabbleView extends VerticalLayout {
	private static final Color SCRABBLE_GREEN = Color.green.darker().darker();
	public static final String CELL_SIZE = "30px";
	private final Grid grid;

	public ScrabbleView() {
		this.grid = new Grid();
		try {
			final ScrabbleRules rules = ScrabbleRulesFactory.create(Language.FRENCH);
			this.grid.play(rules, "A1 ELEPHANT");
		} catch (ScrabbleException.NotParsableException e) {
			throw new Error(e);
		}

		addClassName("scrabble-view");
		setSizeFull();
		final Div div = new Div();
		add(div);
		div.getElement().setProperty("innerHTML", createGridHTML());
	}

	private static String getHTMLColorString(Color color) {
		String red = Integer.toHexString(color.getRed());
		String green = Integer.toHexString(color.getGreen());
		String blue = Integer.toHexString(color.getBlue());

		return "#" +
				(red.length() == 1 ? "0" + red : red) +
				(green.length() == 1 ? "0" + green : green) +
				(blue.length() == 1 ? "0" + blue : blue);
	}

	private String createGridHTML() {
		final CssProperties tableCss = new CssProperties();
		tableCss.put("background-color", "black");
		tableCss.put("border-collapse", "collapse");

		final StringBuilder sb = new StringBuilder();
		sb.append("<table style='").append(tableCss).append("'>");
		for (int y = 0; y < Grid.GRID_SIZE; y++) {
			sb.append("<tr>");
			for (int x = 0; x < Grid.GRID_SIZE; x++) {
				sb.append("<td>").append(getCellHTML(y+1, x+1));
			}
		}
		sb.append("</table>");
		return sb.toString();
	}

	private String getCellHTML(final int y, final int x) {
		final StringBuilder sb = new StringBuilder();
		final Square square = this.grid.get(x, y);

		final CssProperties cellCss = new CssProperties();
		final Color cellColor;
		if (square.tile != null) {
			cellColor = Color.decode("#FFFFF0").darker(); // ivory
			cellCss.put("border-radius", "5px");
		} else if (square.letterBonus == 2) {
			cellColor = Color.decode("0x00BFFF");
		} else if (square.letterBonus == 3) {
			cellColor = Color.blue;
		} else if (square.wordBonus == 2) {
			cellColor = Color.decode("#F6CEF5").darker();
		} else if (square.wordBonus == 3) {
			cellColor = Color.red;
		} else {
			cellColor = SCRABBLE_GREEN;
		}

		cellCss.put("height", CELL_SIZE);
		cellCss.put("width", CELL_SIZE);
		cellCss.put("background-color", getHTMLColorString(cellColor));

		sb.append(String.format(
				"<div align='center' style='%s'>",
				cellCss
			));
		final Tile tile = square.tile;
		sb.append(tile != null ? tile.c : ' ');
		sb.append("</div>");
		return sb.toString();
	}


}