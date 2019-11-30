package com.drvisor.chat.gui;

import org.jgroups.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ChatLauncher extends JFrame {
    private static final Logger LOG = LoggerFactory.getLogger(ChatLauncher.class);
    private static final String TITLE_PREFIX = "JGroups chat";
    private static final String DEFAULT_CHAT_CLUSTER_NAME = "JGroupsChat";
    public static final String DEFAULT_JGROUPS_CONFIG = "udp.xml";
    private JTextArea history;
    private JTextField textField;
    private JList<Address> participants;
    private JChannel jChannel;

    private String userName;

    public static void main(String... args) {
        SwingUtilities.invokeLater(() -> {
            enableNimbusLookAndFeel();
            ChatLauncher frm = new ChatLauncher(args);
            frm.setVisible(true);
        });
    }

    public ChatLauncher(String... args) throws HeadlessException {
        setTitle(TITLE_PREFIX);
        configureUI();

        userName = System.getProperty("usrName");
        if (userName == null) {
            userName = JOptionPane.showInputDialog(null, "Input your chat user name");
            if (userName == null) System.exit(0);
            setTitle(TITLE_PREFIX + " - " + userName);
        }
        LOG.info("Launching with -DusrName={} and args={}", userName, Arrays.toString(args));
        try {
            initJGroups(args);
        } catch (Exception e) {
            LOG.error("Error during jGroups initialization:", e);
        }
    }

    private void configureUI() {
        JPanel pnlMain = new JPanel(new BorderLayout(5, 5));
        JScrollPane sc = new JScrollPane();
        history = new JTextArea();
        textField = new JTextField();
        JPanel footer = new JPanel(new BorderLayout(5, 5));
        SendAction sendAction = new SendAction();
        JButton btnSend = new JButton(sendAction);
        participants = new JList<>();
        participants.setBorder(new TitledBorder("Participants"));
        JScrollPane scParticipants = new JScrollPane();

        history.setEditable(false);
        history.setBorder(new TitledBorder("History"));
        scParticipants.setViewportView(participants);
        sc.setViewportView(history);
        footer.setBorder(new TitledBorder("Message"));
        textField.setAction(sendAction);

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
    }

    private void initJGroups(String[] args) throws Exception {
        String clusterName = (args.length > 0 ? args[0] : DEFAULT_CHAT_CLUSTER_NAME);
        String configName = (args.length > 1 ? args[1] : DEFAULT_JGROUPS_CONFIG);
        LOG.info("Starting with clusterName={}, configName={}", clusterName, configName);
        LOG.info("Usage: java -jar jgroups_chat.jar <clusterName> <configName>");
        jChannel = new JChannel(configName);
        jChannel.setReceiver(new ReceiverAdapter() {
            @Override
            public void receive(Message msg) {
                LOG.debug("received: {}", msg);
                addToHistory(msg.getSrc().toString() + "->" + msg.getDest() + ":" + new String(msg.getBuffer()));
            }

            @Override
            public void viewAccepted(View view) {
                LOG.debug("viewAccepted: {}", view);
                DefaultListModel<Address> participantsModel = new DefaultListModel<>();
                participantsModel.addAll(view.getMembers().stream().filter(v -> !userName.equals(v.toString())).collect(Collectors.toList()));
                participants.setModel(participantsModel);
            }

        });

        jChannel.setName(userName);
        jChannel.setDiscardOwnMessages(true);
        jChannel.connect(clusterName);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> jChannel.close()));

    }

    private void addToHistory(String str) {
        history.append(str + "\n");
        history.setCaretPosition(history.getDocument().getLength());
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
