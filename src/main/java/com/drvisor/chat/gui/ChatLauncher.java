package com.drvisor.chat.gui;

import org.jgroups.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Documentation: http://www.jgroups.org/tutorial5/index.html
 */
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

        userName = args.length > 0 ? args[0] : null;
        if (userName == null) {
            LOG.info("Usage: java -jar jgroups_chat.jar <userName> <clusterName> <configName>");
            userName = JOptionPane.showInputDialog(null, "Input your chat user name");
            if (userName == null) System.exit(0);
        }
        setTitle(TITLE_PREFIX + " - " + userName);

        try {
            initJGroups(args);
        } catch (Exception e) {
            LOG.error("Error during jGroups initialization:", e);
        }
    }

    private void configureUI() {
        JPanel pnlMain = new JPanel(new BorderLayout(5, 5));
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        JScrollPane scHistory = new JScrollPane();
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
        scHistory.setViewportView(history);
        footer.setBorder(new TitledBorder("Message"));
        textField.setAction(sendAction);
        textField.setToolTipText("Press Enter to send");
        splitPane.setDividerLocation(200);
        splitPane.setResizeWeight(1.0);

        setContentPane(pnlMain);
        setMinimumSize(new Dimension(600, 200));

        splitPane.setLeftComponent(scParticipants);
        splitPane.setRightComponent(scHistory);
        pnlMain.add(splitPane, BorderLayout.CENTER);
        footer.add(textField, BorderLayout.CENTER);
        footer.add(btnSend, BorderLayout.EAST);
        pnlMain.add(footer, BorderLayout.SOUTH);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        pack();

        setLocationRelativeTo(null);// put into center
    }

    private void initJGroups(String[] args) throws Exception {
        String clusterName = (args.length > 1 ? args[1] : DEFAULT_CHAT_CLUSTER_NAME);
        String configName = (args.length > 2 ? args[2] : DEFAULT_JGROUPS_CONFIG);
        LOG.info("Starting with clusterName={}, configName={}", clusterName, configName);
        jChannel = new JChannel(configName);
        jChannel.setReceiver(new Receiver() {
            @Override
            public void receive(Message msg) {
                LOG.debug("received: {}", msg);
                addToHistory(msg.getSrc().toString() + "->" + msg.getDest() + ":" + msg.getObject());
                ChatLauncher.this.setVisible(true);
                ChatLauncher.this.requestFocus();
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
        history.append(LocalDateTime.now() + " " + str + "\n");
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
            Message msg = new ObjectMessage(selected, textField.getText());
            try {
                jChannel.send(msg);
            } catch (Exception e) {
                LOG.error("Error while sending", e);
            }
            addToHistory("Me->" + selected + ":" + textField.getText());
            textField.setText("");
        }
    }
}
