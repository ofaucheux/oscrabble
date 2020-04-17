//package com.zetcode;
//
//import javafx.scene.input.DragEvent;
//
//import javax.swing.*;
//import java.awt.*;
//import java.awt.dnd.DropTarget;
//import java.awt.dnd.DropTargetAdapter;
//import java.awt.dnd.DropTargetDragEvent;
//import java.awt.dnd.DropTargetDropEvent;
//import java.awt.event.MouseAdapter;
//import java.awt.event.MouseEvent;
//import java.util.TooManyListenersException;
//
//public class IconDnD extends JFrame {
//
//    public IconDnD() throws TooManyListenersException
//    {
//
//        initUI();
//    }
//
//    private void initUI() throws TooManyListenersException
//    {
//
//        var icon1 = new ImageIcon("C:\\Programmierung\\OScrabble\\src\\main\\resources\\checkboxBlack.png");
//        var icon2 = new ImageIcon("C:\\Programmierung\\OScrabble\\src\\main\\resources\\checkboxFalse.png");
//        var icon3 = new ImageIcon("C:\\Programmierung\\OScrabble\\src\\main\\resources\\checkboxTrue.png");
//
//        var label1 = new JLabel("ICi", JLabel.CENTER);
//        var label2 = new JLabel(icon2, JLabel.CENTER);
//        var label3 = new JLabel(icon3, JLabel.CENTER);
//
//        var listener = new DragMouseAdapter();
//        label1.addMouseListener(listener);
//        label2.addMouseListener(listener);
//        label3.addMouseListener(listener);
//
//
////        var button = new JPanel();
////        button.setBackground(Color.GRAY);
////        button.setFocusable(false);
////        button.setDropTarget(new DropTarget());
//
//        label1.setTransferHandler(new TransferHandler("icon"));
//        label2.setTransferHandler(new TransferHandler("icon"));
//        label3.setTransferHandler(new TransferHandler("icon"));
////        button.setTransferHandler(new TransferHandler("icon"));
//
//        createLayout(label1, label2, label3, null);
//
//        setTitle("Icon Drag & Drop");
//        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        setLocationRelativeTo(null);
//
//        final JFrame ziel = new JFrame("Ziel");
//        ziel.setSize(100, 100);
//        ziel.setVisible(true);
//        ziel.addMouseListener(listener);
//        ziel.setTransferHandler(new TransferHandler("icon"));
//        final DropTarget dt = new DropTarget();
//        dt.addDropTargetListener(new DropTargetAdapter()
//        {
//            @Override
//            public void drop(final DropTargetDropEvent dtde)
//            {
////                JOptionPane.showMessageDialog(null, "Done!");
//            }
//
//            @Override
//            public void dragEnter(final DropTargetDragEvent dtde)
//            {
//                SwingUtilities.invokeLater(() ->
//                        ziel.add(new Label("ici!"))
//                );
//            }
//        });
//        ziel.setDropTarget(dt);
//    }
//
//    private class DragMouseAdapter extends MouseAdapter {
//
//        public void mousePressed(MouseEvent e) {
//
//            var c = (JComponent) e.getSource();
//            var handler = c.getTransferHandler();
//            handler.exportAsDrag(c, e, TransferHandler.COPY);
//        }
//    }
//
//    private void createLayout(JComponent... arg) {
//
//        var pane = getContentPane();
//        var gl = new GroupLayout(pane);
//        pane.setLayout(gl);
//
//        gl.setAutoCreateContainerGaps(true);
//        gl.setAutoCreateGaps(true);
//
//        gl.setHorizontalGroup(gl.createParallelGroup(GroupLayout.Alignment.CENTER)
//                .addGroup(gl.createSequentialGroup()
//                        .addComponent(arg[0])
//                        .addGap(30)
//                        .addComponent(arg[1])
//                        .addGap(30)
//                        .addComponent(arg[2])
//                )
////                .addComponent(arg[3], GroupLayout.DEFAULT_SIZE,
////                        GroupLayout.DEFAULT_SIZE, Integer.MAX_VALUE)
//        );
//
//        gl.setVerticalGroup(gl.createSequentialGroup()
//                .addGroup(gl.createParallelGroup()
//                        .addComponent(arg[0])
//                        .addComponent(arg[1])
//                        .addComponent(arg[2]))
//                .addGap(30)
////                .addComponent(arg[3])
//        );
//
//        pack();
//    }
//
//    public static void main(String[] args) {
//
//        EventQueue.invokeLater(() -> {
//
//            IconDnD ex = null;
//            try
//            {
//                ex = new IconDnD();
//                ex.setVisible(true);
//            }
//            catch (TooManyListenersException e)
//            {
//                e.printStackTrace();
//            }
//        });
//    }
//}