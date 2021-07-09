
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;

public class SequenceMatchLogGui {
    private JPanel panel;
    private JButton button1;
    private JTable table1;
    private JCheckBox reader1SequenceCheckCheckBox;
    private JPanel reader1ConfigurationPanel;
    private JCheckBox reader2ActiveCheckBox;
    private JCheckBox reader2SequenceCheckCheckBox;
    private JTextPane massagesTextPane;
    private JCheckBox inFileCheckBox;
    private JButton diagButton;
    private JLabel directionLable;
    private JLabel maxNoReadLable;
    private JTextField fileTextField;
    private JButton chooseFileButton;
    private JCheckBox startWithOneCheckBox;
    String reader1Name, reader2Name;
    private HashSet<String> errorsBarcodes;
    private Manager manager;

    private static Logger logger = LogManager.getLogger("Gui");

    public SequenceMatchLogGui() throws IOException {
//        logger.warn ("ho");
//        init errorBarcodes set :
        errorsBarcodes = new HashSet<>();
        errorsBarcodes.add(PropertiesHandler.getProperty("noReadString", "noRead"));
//        errorsBarcodes.add("");// should blank cells be red ??
        reader1Name = "reader1";//PropertiesHandler.getProperty("reader1Name", "reader1");
        reader2Name = "reader2";//PropertiesHandler.getProperty("reader2Name", "reader2");
        DefaultTableModel model = (DefaultTableModel) table1.getModel();
        model.setColumnCount(3);
//        model.setRowCount(3);doesn't work//'
//        set columns names
        table1.getColumnModel().getColumn(0).setHeaderValue(reader1Name);
        table1.getColumnModel().getColumn(1).setHeaderValue(reader2Name);
        table1.getColumnModel().getColumn(2).setHeaderValue("time");
        table1.setRowHeight(Integer.parseInt(PropertiesHandler.getProperty("fontSize", "30")));
//        "noRead"" will be red
//        table1.setDefaultRenderer(Object.class, new StatusColumnCellRenderer());
        StatusColumnCellRenderer statusColumnCellRenderer = new StatusColumnCellRenderer();
        table1.getColumnModel().getColumn(0).setCellRenderer(statusColumnCellRenderer);
        table1.getColumnModel().getColumn(1).setCellRenderer(statusColumnCellRenderer);
//        set checkboxs:
//        reader1SequenceCheckCheckBox.setText(reader1Name + " sequence check");
        reader1SequenceCheckCheckBox.setSelected(PropertiesHandler.getProperty("reader1SequenceCheck", "true").equals("true"));
        inFileCheckBox.setSelected(PropertiesHandler.getProperty("inFileCheck", "false").equals("true"));
//        reader2ActiveCheckBox.setText(reader2Name + " active");
        reader2ActiveCheckBox.setSelected(PropertiesHandler.getProperty("reader2Active", "true").equals("true"));
        startWithOneCheckBox.setSelected(PropertiesHandler.getProperty("barcodeStartWithOne", "true").equals("true"));
        manager = new Manager(this);
        directionLable.setText(PropertiesHandler.getProperty("ascending", "false").equals("true") ? "כיוון עולה" : "כיוון יורד");
        maxNoReadLable.setText("חוסר קריאה מותר : " + PropertiesHandler.getProperty("maxNoReadAllowed", "0"));
//        align massages to the right:
        SimpleAttributeSet attribs = new SimpleAttributeSet();
        StyleConstants.setAlignment(attribs, StyleConstants.ALIGN_RIGHT);
        massagesTextPane.setParagraphAttributes(attribs, false);
        fileTextField.setText(new File(PropertiesHandler.getProperty("file", "")).getName());
        if (inFileCheckBox.isSelected())
            manager.fillBarcodesSet(PropertiesHandler.getProperty("file", ""));
        setLogFileName(fileTextField.getText());
//        add first empty row
        addNewEmptyRowToTable();


        reader1SequenceCheckCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    PropertiesHandler.setProperty("reader1SequenceCheck", reader1SequenceCheckCheckBox.isSelected() ? "true" : "false");
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });
        reader2ActiveCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    PropertiesHandler.setProperty("reader2Active", reader2ActiveCheckBox.isSelected() ? "true" : "false");
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });


        diagButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Diag(manager);
            }
        });
        chooseFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    chooseFile();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });
        button1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resizeColumnWidth(table1);
//                logger.warn ("ho");
//                manager.serialEvent("io", "128");//rising
//                manager.serialEvent("io", "0");//falling

            }


        });

        inFileCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    PropertiesHandler.setProperty("inFileCheck", String.valueOf(inFileCheckBox.isSelected()));
                    if (inFileCheckBox.isSelected())
                        manager.fillBarcodesSet(PropertiesHandler.getProperty("file", ""));
                    setLogFileName(fileTextField.getText());
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });
    }


    public void addToErrorsSet(String barcode) {
        errorsBarcodes.add(barcode);
    }

    public void refreshTableView() {
        ((DefaultTableModel) table1.getModel()).fireTableDataChanged();
    }

    public void addMassage(String massage) {
        SimpleAttributeSet attributeSet = new SimpleAttributeSet();
//        StyleConstants.setFontSize(attributeSet, 20);
        try {
            massagesTextPane.getStyledDocument().insertString(0, massage+System.lineSeparator(), attributeSet);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        massagesTextPane.setCaretPosition(0);
    }

//    private void addRow(DefaultTableModel model, String reader1, String Reader2) {
//        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SSS");
//        Date date = new Date();
//        String dateAndTime = formatter.format(date);
//        String[] data = new String[]{reader1, Reader2, dateAndTime};
//        model.addRow(data);
//    }

    public void updateRow0(String readerName, String data) {

        DefaultTableModel model = (DefaultTableModel) table1.getModel();
        int column = table1.getColumnModel().getColumnIndex(readerName);
//        if (!model.getValueAt(0, column).equals("")) //the cell is not empty - this happen only if read without trigger received...
//            model.insertRow(0, new String[]{"", "", ""});
        model.setValueAt(data, 0, column);
    }

    public void addNewEmptyRowToTable() {
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss:SSS");
        Date date = new Date();
        String dateAndTime = formatter.format(date);
        String[] data = new String[]{"", "", dateAndTime};
        ((DefaultTableModel) table1.getModel()).insertRow(0, data);
    }

    public boolean isStartWithOne() {
        return startWithOneCheckBox.isSelected();
    }

    public class StatusColumnCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            //Cells are by default rendered as a JLabel.
            JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            if (col == 1 && !reader2ActiveCheckBox.isSelected()) {
                l.setBackground(Color.white);
                l.setForeground(Color.LIGHT_GRAY);
            } else {
                l.setForeground(Color.black);
                if (errorsBarcodes.contains(value))
                    l.setBackground(Color.RED);
                else
                    l.setBackground(Color.white);
            }
            //Return the JLabel which renders the cell.
            return l;
        }
    }

    private void chooseFile() throws IOException {
        String filePath = PropertiesHandler.getProperty("file", "");
//        JFileChooser chooser = new JFileChooser(filePath);
        JFileChooser chooser = new JFileChooser(filePath);//"s://plus");
        chooser.setPreferredSize(new Dimension(700, 500));
//        FileNameExtensionFilter filter = new FileNameExtensionFilter(
//            "job files", "job");
//        chooser.setFileFilter(filter);

//        cosumize chooser filter :
        JTextField tf = (JTextField) ((JPanel) ((JPanel) chooser.getComponent(3)).getComponent(0)).getComponent(1);
        tf.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                modifyFilter(chooser, tf);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                modifyFilter(chooser, tf);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                modifyFilter(chooser, tf);
            }
        });
        int returnVal = chooser.showOpenDialog(panel);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            filePath = chooser.getSelectedFile().getAbsolutePath();
            File f = new File(filePath);
            if (f.exists() && !f.isDirectory()) {
                String fileName = chooser.getSelectedFile().getName();
                System.out.println("filePath: " + filePath);
                System.out.println("fileName: " + fileName);
                PropertiesHandler.setProperty("file", filePath);
//                storeLastConfiguration("filePath", filePath);
//                storeLastConfiguration("fileName", fileName);
                fileTextField.setText(fileName);
                manager.fillBarcodesSet(filePath);
//                runButton.setEnabled(true);
//                rprButton.setEnabled(true);
                setLogFileName(fileTextField.getText());
            } else {
                JOptionPane.showMessageDialog(null, "choose again", "  file not found ", JOptionPane.INFORMATION_MESSAGE);
                chooseFile();
            }
        }
    }

    private void setLogFileName(String logFileName) {
//        if not working with file - set file name to this month.year
//        all files will append massages
//        set log file name
        if (inFileCheckBox.isSelected()) {  // log file name is working file name
            System.setProperty("logFileName", logFileName+".log");
        } else { // log file name by month (MM-YYYY)
            System.setProperty("logFileName", new SimpleDateFormat("MM-yyyy").format(new Date())+".log");
        }
        org.apache.logging.log4j.core.LoggerContext ctx =
                (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
        ctx.reconfigure();
    }

    //    try {
//        ProviderRegistry.getLoggingProvider().shutdown();
//    } catch (InterruptedException e) {
//        e.printStackTrace();
//    }
    private void modifyFilter(JFileChooser chooser, JTextField tf) {
        final String text = tf.getText();
        chooser.setFileFilter(new FileFilter() {
            @Override
            public String getDescription() {
                return text;
            }

            @Override
            public boolean accept(File f) {
                return (f.isDirectory() || f.getName().toLowerCase().startsWith(text.toLowerCase()));
            }
        });
    }

    public boolean isReader1SequenceCheckActive() {
        return reader1SequenceCheckCheckBox.isSelected();
    }

    public boolean isReader2Active() {
        return reader2ActiveCheckBox.isSelected();
    }

    public boolean isReader1FileCheckActive() {
        return inFileCheckBox.isSelected();
    }

    public void resizeColumnWidth(JTable table) {
        final TableColumnModel columnModel = table.getColumnModel();
        for (int column = 0; column < table.getColumnCount(); column++) {
            int width = 15; // Min width
            for (int row = 0; row < table.getRowCount(); row++) {
                TableCellRenderer renderer = table.getCellRenderer(row, column);
                Component comp = table.prepareRenderer(renderer, row, column);
                width = Math.max(comp.getPreferredSize().width + 1, width);
            }
            if (width > 300)
                width = 300;
            columnModel.getColumn(column).setPreferredWidth(width);
        }
    }

    public static void main(String[] args) throws IOException {
        setUIFont(new javax.swing.plaf.FontUIResource("Serif", Font.PLAIN, Integer.parseInt(PropertiesHandler.getProperty("fontSize", "30"))));
        JFrame frame = new JFrame("sequenceMatchLogGuiCmc250");
        frame.setContentPane(new SequenceMatchLogGui().panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.pack();
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
//        frame.setUndecorated(true);
        frame.setVisible(true);
    }

    public static void setUIFont(javax.swing.plaf.FontUIResource f) {
        java.util.Enumeration keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof javax.swing.plaf.FontUIResource)
                UIManager.put(key, f);
        }
    }

}
