package com.drvisor.chat.gui;

import org.jgroups.*;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

public class ChatLauncher extends JFrame {
    private JPanel pnlMain;
    private JScrollPane sc;
    private JTextArea history;
    private JPanel footer;
    private JTextField textField;
    private JButton btnSend;
    private JList<Address> participants;
    private JScrollPane scParticipants;
    private JChannel jChannel;

    public static void main(String... args) {
        SwingUtilities.invokeLater(() -> {
            enableNimbusLookAndFeel();
            ChatLauncher frm = new ChatLauncher();
            frm.setVisible(true);
        });
    }

    public ChatLauncher() throws HeadlessException {
        setTitle("JGroups chat");

        pnlMain = new JPanel(new BorderLayout(5, 5));
        sc = new JScrollPane();
        history = new JTextArea();
        textField = new JTextField();
        footer = new JPanel(new BorderLayout(5, 5));
        btnSend = new JButton(new SendAction());
        participants = new JList<>();
        participants.setBorder(new TitledBorder("Participants"));
        scParticipants = new JScrollPane();

        history.setEditable(false);
        history.setBorder(new TitledBorder("History"));
        scParticipants.setViewportView(participants);
        sc.setViewportView(history);
        footer.setBorder(new TitledBorder("Message"));

        setContentPane(pnlMain);
        setMinimumSize(new Dimension(600, 200));

        pnlMain.add(scParticipants, BorderLayout.WEST);
        pnlMain.add(sc, BorderLayout.CENTER);
        pnlMain.add(footer, BorderLayout.SOUTH);
        footer.add(textField, BorderLayout.CENTER);
        footer.add(btnSend, BorderLayout.EAST);


        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        pack();

        setLocationRelativeTo(null);// put into center

        try {
            initJGroups();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initJGroups() throws Exception {
        jChannel = new JChannel("udp.xml");
        jChannel.setReceiver(new ReceiverAdapter() {
            @Override
            public void receive(Message msg) {
                System.out.println("received");
                addToHistory(msg.getSrc().toString() + "->" + msg.getDest() + ":" + new String(msg.getBuffer()));
            }

            @Override
            public void viewAccepted(View view) {
                System.out.println(view);
                DefaultListModel<Address> participantsModel = new DefaultListModel<>();
                participantsModel.addAll(view.getMembers());
                participants.setModel(participantsModel);
            }
        });
        String userName = System.getProperty("usrName");
        if (userName == null) {
            userName = JOptionPane.showInputDialog(null, "Input your participant name");
            if (userName == null) System.exit(0);
        }
        jChannel.setName(userName);
        jChannel.setDiscardOwnMessages(true);
        jChannel.connect("JGroupsChat");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> jChannel.close()));

    }

    private void addToHistory(String str) {
        history.append(str + "\n");
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

    private class SendAction extends AbstractAction {
        public SendAction() {
            putValue(NAME, "Send");
            putValue(SHORT_DESCRIPTION, "Send message into chat");
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            Address selected = participants.getSelectedValue();
            Message msg = new Message(selected, textField.getText());
            try {
                jChannel.send(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
            addToHistory("Me->" + selected + ":" + textField.getText());
        }
    }
}
