import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

public class Diag {
    private JTextField dataTextField;
    private JComboBox comboBox1;
    private JButton threadsButton;
    private JPanel panel;
    private JTextField reader1TextField;
    private JButton reader1Button;
    private JTextField reader2TextField;
    private JButton reader2Button;
    private JButton triggerButton;
    private JButton toothEyeButton;
    private JButton checkerEyeButton;
    private JTable table1;
    Manager manager;

    public Diag(Manager manager) {
        this.manager = manager;
        JFrame frame = new JFrame("Diag");
        frame.setContentPane(panel);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        manager.setDiag(this);
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("inputs");
        table1.setModel(model);
        for (int i = 0; i < 12; i++) {
            Integer[] a = {i};
            model.addRow(a);
        }
        StatusColumnCellRenderer statusColumnCellRenderer = new StatusColumnCellRenderer();
        table1.getColumnModel().getColumn(0).setCellRenderer(statusColumnCellRenderer);
        try {
            table1.setRowHeight(Integer.parseInt(PropertiesHandler.getProperty("fontSize", "30")));
        } catch (IOException e) {
            e.printStackTrace();
        }

        threadsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ThreadGroup currentGroup = Thread.currentThread().getThreadGroup();
                //        Thread.currentThread().setPriority(1);
//                System.out.println("Thread.currentThread().getName(): " + Thread.currentThread().getName());
                int nuThreads = currentGroup.activeCount();
                System.out.println("number of Threads : " + nuThreads);
                Thread[] lstThreads = new Thread[nuThreads];
                currentGroup.enumerate(lstThreads);
                for (int i = 0; i < nuThreads; i++) {
                    System.out.println("Thread No:" + i + " = " + lstThreads[i].getName()
                            + " priority: " + lstThreads[i].getPriority());
                }
            }
        });


        reader1Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                manager.serialEvent("reader1", reader1TextField.getText());
                reader1TextField.setText(String.valueOf(Integer.parseInt(reader1TextField.getText()) + 1));
            }
        });
        reader2Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                manager.serialEvent("reader2", reader2TextField.getText());
                reader2TextField.setText(String.valueOf(Integer.parseInt(reader2TextField.getText()) + 1));
            }
        });
        triggerButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                try {
                    String inputsMap= String.valueOf((int)Math.pow(2,Integer.parseInt(PropertiesHandler.getProperty("feederEyeInputNumber", "7"))));
                    manager.serialEvent("io", inputsMap);//rising
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }

            }
        });
        triggerButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                manager.serialEvent("io", "0");//falling
            }
        });


        toothEyeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                try {
                    String inputsMap= String.valueOf((int)Math.pow(2,Integer.parseInt(PropertiesHandler.getProperty("advanceCycleInputNumber", "8"))));
                    manager.serialEvent("io", inputsMap);//rising
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });
        toothEyeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                manager.serialEvent("io", "0");//falling
            }
        });
        checkerEyeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                try {
                    String inputsMap= String.valueOf((int)Math.pow(2,Integer.parseInt(PropertiesHandler.getProperty("checkerEyeInputNumber", "9"))));
                    manager.serialEvent("io", inputsMap);//rising
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });
        checkerEyeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                manager.serialEvent("io", "0");//falling
            }
        });
    }



    Input[] inputs;
    public void updateInputsState(Input[] inputs) {
//        for (int i = 0; i < inputs.length; i++) {
//            table1.getModel().setValueAt(inputs[i].currentState);
//        }
        this.inputs=inputs;
        ((DefaultTableModel) table1.getModel()).fireTableDataChanged();
    }

    public class StatusColumnCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            //Cells are by default rendered as a JLabel.
            JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            l.setForeground(Color.black);
            int i=(Integer)value;
            if ( inputs!=null&&inputs[i].currentState)
                l.setBackground(Color.RED);
            else
                l.setBackground(Color.green);

            //Return the JLabel which renders the cell.
            return l;
        }
    }
}
