import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
    Manager manager;

    public Diag(Manager manager) {
        this.manager=manager;
        JFrame frame = new JFrame("Diag");
        frame.setContentPane(panel);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
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
                            +" priority: "+ lstThreads[i].getPriority());
                }
            }
        });


        reader1Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                manager.serialEvent("reader1",reader1TextField.getText());
                reader1TextField.setText(String.valueOf(Integer.parseInt(reader1TextField.getText())+1));
            }
        });
        reader2Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                manager.serialEvent("reader2",reader2TextField.getText());
                reader2TextField.setText(String.valueOf(Integer.parseInt(reader2TextField.getText())+1));
            }
        });
        triggerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                manager.serialEvent("io","bla");
            }
        });
    }

//    public static void main(String[] args) {


//            SwingUtilities.invokeLater(new Runnable() {
//                public void run() {
//                    JFrame frame = new JFrame("Diag");
//                    frame.setContentPane(new Diag().panel);
//                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//                    frame.pack();
//                    frame.setVisible(true);
//                }
//            });

//            JFrame frame = new JFrame("Diag");
//            frame.setContentPane(new Diag().panel);
//            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//            frame.pack();
//            frame.setVisible(true);

//    }
    //    public Diag(Manager manager) {
//        this.manager = manager;
//        sendButton.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                manager.serialEvent((String) comboBox1.getSelectedItem(),dataTextField.getText());
//            }
//        });
//    }
}
