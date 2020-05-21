//package oscrabble.client;
//
//import oscrabble.data.objects.Grid;
//import oscrabble.player.ai.BruteForceMethod;
//
//import javax.swing.*;
//import java.awt.*;
//import java.awt.event.ActionEvent;
//import java.awt.event.MouseAdapter;
//import java.awt.event.MouseEvent;
//import java.util.ArrayList;
//import java.util.Comparator;
//import java.util.Set;
//import java.util.Vector;
//
///**
// * This action display the list of possible and authorized moves.
// */
//class PossibleMoveDisplayer extends AbstractAction
//{
//
//	private final Playground playground;
//
//	private final BruteForceMethod bruteForceMethod;
//
//	/**
//	 * Group of buttons for the order
//	 */
//	private final ButtonGroup orderButGroup;
//
//	/**
//	 * List of legal moves
//	 */
//	private ArrayList<String> legalMoves;
//
//	/**
//	 * Swing list of sorted possible moves
//	 */
//	private final JList<String> moveList;
//
//	PossibleMoveDisplayer(final Playground playground, final BruteForceMethod bruteForceMethod)
//	{
//		super(Playground.LABEL_DISPLAY);
//		this.playground = playground;
//		this.bruteForceMethod = bruteForceMethod;
//		this.orderButGroup = new ButtonGroup();
//		this.moveList = new JList<>();
//		this.moveList.setCellRenderer(new DefaultListCellRenderer()
//		{
//			@Override
//			public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus)
//			{
//				final Component label = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
//				if (value instanceof String)
//				{
//					final Grid.MoveMetaInformation mmi = (Grid.MoveMetaInformation) value;
//					this.setText(mmi.getPlayTiles().toString() + "  " + mmi.getScore() + " pts");
//				}
//				return label;
//			}
//		});
//
//		this.moveList.addListSelectionListener(event -> {
//			PlayTiles playTiles = null;
//			for (int i = event.getFirstIndex(); i <= event.getLastIndex(); i++)
//			{
//				if (this.moveList.isSelectedIndex(i))
//				{
//					playTiles = this.moveList.getModel().getElementAt(i).getPlayTiles();
//					break;
//				}
//			}
//			playground.jGrid.highlightMove(playTiles);
//		});
//
//		this.moveList.addMouseListener(new MouseAdapter()
//		{
//			@Override
//			public void mouseClicked(final MouseEvent e)
//			{
//				if (e.getClickCount() >= 2)
//				{
//					new SwingWorker<>()
//					{
//						@Override
//						protected Object doInBackground() throws Exception
//						{
//							Thread.sleep(100);  // let time to object to be selected by other listener
//							final List<Grid.MoveMetaInformation> selection = PossibleMoveDisplayer.this.moveList.getSelectedValuesList();
//							if (selection.size() != 1)
//							{
//								return null;
//							}
//
//							final PlayTiles playTiles = selection.get(0).getPlayTiles();
//							playground.play(playTiles);
//							playground.commandPrompt.setText("");
//
//							return null;
//						}
//					}.execute();
//				}
//			}
//		});
//	}
//
//	@Override
//	public void actionPerformed(final ActionEvent e)
//	{
//		try
//		{
//			if (this.moveList.isDisplayable())
//			{
//				Playground.resetPossibleMovesPanel();
//				Playground.showPossibilitiesButton.setText(Playground.LABEL_DISPLAY);
//				return;
//			}
//
//			final Set<PlayTiles> playTiles = this.bruteForceMethod.getLegalMoves(playground.getGrid(),
//					playground.game.getRack(player, player.getPlayerKey()));
//			this.legalMoves = new ArrayList<>();
//
//			for (final PlayTiles playTile : playTiles)
//			{
//				this.legalMoves.add(playground.getGrid().getMetaInformation(playTile));
//			}
//
//			Playground.possibleMovePanel.add(
//					new JScrollPane(this.moveList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED)
//			);
//
//			final JPanel orderMethodPanel = new JPanel();
//			Playground.possibleMovePanel.add(orderMethodPanel, BorderLayout.NORTH);
//			orderMethodPanel.add(new OrderButton(Playground.MESSAGES.getString("score"), Grid.MoveMetaInformation.SCORE_COMPARATOR));
//			orderMethodPanel.add(new OrderButton(Playground.MESSAGES.getString("length"), Grid.MoveMetaInformation.WORD_LENGTH_COMPARATOR));
//			this.orderButGroup.getElements().asIterator().next().doClick();
//			Playground.possibleMovePanel.validate();
//		}
//		catch (ScrabbleException e1)
//		{
//			e1.printStackTrace();
//		}
//
//		Playground.showPossibilitiesButton.setText(Playground.LABEL_HIDE);
//	}
//
//	/**
//	 * Radio button for the selection of the order of the word list.
//	 */
//	private class OrderButton extends JRadioButton
//	{
//		final Comparator<Grid.MoveMetaInformation> comparator;
//
//		private OrderButton(final String label, final Comparator<Grid.MoveMetaInformation> comparator)
//		{
//			super();
//			this.comparator = comparator;
//
//			PossibleMoveDisplayer.this.orderButGroup.add(this);
//			setAction(new AbstractAction(label)
//			{
//				@Override
//				public void actionPerformed(final ActionEvent e)
//				{
//					PossibleMoveDisplayer.this.legalMoves.sort(OrderButton.this.comparator.reversed());
//					PossibleMoveDisplayer.this.moveList.setListData(new Vector<>(PossibleMoveDisplayer.this.legalMoves));
//				}
//			});
//		}
//	}
//}
