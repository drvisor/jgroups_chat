package com.drvisor.chat.gui;

import javax.swing.*;
import java.awt.*;

public class ChatLauncher extends JFrame {
    private JPanel pnlMain;

    public static void main(String ... args){
        SwingUtilities.invokeLater(() -> {
            enableNimbusLookAndFeel();
            ChatLauncher frm = new ChatLauncher();
            frm.setVisible(true);
        });
    }

    public ChatLauncher() throws HeadlessException {
        setTitle("JGroups chat");
        pnlMain = new JPanel(new BorderLayout(5,5));
        setContentPane(pnlMain);
        setMinimumSize(new Dimension(200,100));

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        pack();

        setLocationRelativeTo(null);// put into center
    }

    private static void enableNimbusLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // If Nimbus is not available, you can set the GUI to another look and feel.
        }
    }
}
