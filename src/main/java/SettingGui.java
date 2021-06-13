import javax.swing.*;

public class SettingGui {
    private JCheckBox reader1SequenceCheckCheckBox;
    private JCheckBox reader2ActiveCheckBox;
    private JCheckBox reader1MatchToFileCheckBox;
    private JPanel settingsPanel;
    private SequenceMatchLogGui mainGui;

    public SettingGui(SequenceMatchLogGui mainGui) {
        this.mainGui = mainGui;
        JFrame frame = new JFrame("SettingGui");
        frame.setContentPane(settingsPanel);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) {

    }
}
