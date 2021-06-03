package ru.geekBrains;

import ru.geekBrains.histoty.SaveHistory;
import ru.geekbrains.messages.MessageDTO;
import ru.geekbrains.messages.MessageType;

import javax.swing.*;
import javax.swing.text.StyledEditorKit;
import java.awt.*;
import java.io.IOException;

public class NewChat extends JFrame implements MessageProcessor {

    private static final int WIDTH = 500;
    private static final int HEIGHT = 500;

    JMenuBar menuBar = new JMenuBar();
    JMenu menu = new JMenu("Menu");
    JMenu help = new JMenu("Help");
    final JMenuItem connect = menu.add(new StyledEditorKit.ForegroundAction("Connect", Color.LIGHT_GRAY));
    final JMenuItem IPAdd = menu.add(new StyledEditorKit.ForegroundAction("IPAddress", Color.LIGHT_GRAY));
    final JMenuItem jMenuItemPort = menu.add(new StyledEditorKit.ForegroundAction("Port", Color.LIGHT_GRAY));
    final JMenuItem nickname = menu.add(new StyledEditorKit.ForegroundAction("Change user name", Color.LIGHT_GRAY));
    final JMenuItem mail = help.add(new StyledEditorKit.ForegroundAction("Support@mail.com", Color.LIGHT_GRAY));

    private final JPanel panelBottomUp = new JPanel(new GridLayout(1,5));
    private final JTextField tfLogin = new JTextField();
    private final JTextField tfPassword = new JTextField();

    private final JTextArea chat = new JTextArea();

    private final JPanel panelBottom = new JPanel(new BorderLayout());
    private final JTextField tfMessage = new JTextField();

    private MessageService messageService;
    private final SaveHistory saveHistory = new SaveHistory();

    private int port = 8189;
    private String ip = "localhost";

    private final JList<String> userList = new JList<>();
    private final String ALL = "SEND TO ALL";

    public NewChat(){

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);
        chat.setEditable(false);
        userList.setBackground(Color.LIGHT_GRAY);
        chat.setBackground(Color.LIGHT_GRAY);

        add(menuBar, BorderLayout.NORTH);
        menuBar.add(menu);
        menuBar.add(help);

        JLabel lLogin = new JLabel("UserName");
        panelBottomUp.add(lLogin);
        panelBottomUp.add(tfLogin);
        JLabel lPassword = new JLabel("Password");
        panelBottomUp.add(lPassword);
        panelBottomUp.add(tfPassword);
        JButton auth = new JButton("Auth");
        panelBottomUp.add(auth);

        JScrollPane scrollChat = new JScrollPane(chat);
        JScrollPane scrollUser = new JScrollPane(userList);
        scrollUser.setPreferredSize(new Dimension(100, 0));

        panelBottom.add(tfMessage, BorderLayout.CENTER);
        JButton btnSend = new JButton("Send");
        panelBottom.add(btnSend, BorderLayout.EAST);
        panelBottom.setVisible(false);

        JPanel panelMain = new JPanel(new BorderLayout());
        add(panelMain, BorderLayout.CENTER);
        panelMain.add(scrollChat, BorderLayout.CENTER);
        panelMain.add(scrollUser, BorderLayout.EAST);
        panelMain.add(panelBottom, BorderLayout.SOUTH);
        panelMain.add(panelBottomUp,BorderLayout.NORTH);

        tfMessage.addActionListener(e -> sendMessage());

        btnSend.addActionListener(e -> sendMessage());

        jMenuItemPort.addActionListener(e -> {
            String result = JOptionPane.showInputDialog(
                    NewChat.this,"Введите port");
            chat.append(result);
            port = Integer.parseInt(result);
        });

        connect.addActionListener(e -> {
            if(IPAdd != null ) {
                try {
                    messageService = new ChatMessageService(ip, port, NewChat.this);
                    panelBottomUp.setVisible(true);
                } catch (IOException ioException) {
                    chat.append("no connection to the server...\n");
                }
            }

        });

        IPAdd.addActionListener(e -> {
            String result = JOptionPane.showInputDialog(
                   NewChat.this,"Введите IP Address");
            ip = result;
            chat.append(result);
        });

        nickname.addActionListener(e -> {
            String result = JOptionPane.showInputDialog(
                    NewChat.this,"Введите новое имя");
            changeNickname(result);
        });

        auth.addActionListener(e -> sendAuth());

        try {
            messageService = new ChatMessageService(ip, port, this);
        } catch (IOException e) {
            chat.append("no connection to the server...\n");
        }

        setVisible(true);
    }

    @Override
    public void processMessage(String msg) {
        MessageDTO dto = MessageDTO.convertFromJson(msg);
        System.out.println("Received message");
        switch (dto.getMessageType()) {
            case PUBLIC_MESSAGE,PRIVATE_MESSAGE,SERVICE_MESSAGE -> showMessage(dto);
            case CLIENTS_LIST_MESSAGE -> refreshUserList(dto);
            case ERROR_MESSAGE -> showError(dto);
            case AUTH_CONFIRM -> {
                panelBottomUp.setVisible(false);
                panelBottom.setVisible(true);
                chat.setText(null);
                saveHistory.openFile(dto.getLogin());
                showHistory(dto.getLogin());
            }
        }
    }

    private void sendMessage(){
        String message = tfMessage.getText();
        if(message.length() == 0) return;

        MessageDTO dto = new MessageDTO();
        String selected = userList.getSelectedValue();
        if(selected.equals(ALL)) dto.setMessageType(MessageType.PUBLIC_MESSAGE);
        else {
            dto.setMessageType(MessageType.PRIVATE_MESSAGE);
            dto.setTo(selected);
        }
        dto.setBody(message);
        try {
            messageService.sendMessage(dto.convertToJson());
        } catch (IOException e) {
            chat.append("no connection to the server...\n");
        }
        tfMessage.setText("");
    }

    private void showMessage(MessageDTO message) {
        String msg = String.format("[%s] [%s] -> %s\n", message.getMessageType(),message.getFrom(), message.getBody());
        chat.append(msg);
        saveHistory.writingToFile(msg);
    }

    private void showHistory(String login){
        String msg = saveHistory.readFromFile(login);
        chat.append(msg + "\n");
    }

      public void sendAuth() {
        String log = tfLogin.getText();
        String pass = tfPassword.getText();
        if (log.equals("") || pass.equals("")) return;
        MessageDTO dto = new MessageDTO();
        dto.setLogin(log);
        dto.setPassword(pass);
        dto.setMessageType(MessageType.SEND_AUTH_MESSAGE);
          try {
              messageService.sendMessage(dto.convertToJson());
          } catch (IOException e) {
              chat.append("no connection to the server...\n");
          }
          System.out.println("Sent " + log + " " + pass);
    }

    public void changeNickname(String newNickname){
        if (newNickname.equals("")) return;
        MessageDTO dto = new MessageDTO();
        dto.setBody(newNickname);
        dto.setMessageType(MessageType.SERVICE_MESSAGE);
        try {
            messageService.sendMessage(dto.convertToJson());
        } catch (IOException e) {
            chat.append("no connection to the server...\n");
        }
        System.out.println("Sent " + newNickname);
    }

    private void refreshUserList(MessageDTO dto){
        dto.getUserOnline().add(0,ALL);
        userList.setListData(dto.getUserOnline().toArray(new String[0]));
        userList.setSelectedIndex(0);
    }

    private void showError(MessageDTO dto){
                JOptionPane.showMessageDialog(NewChat.this, dto.getBody());
    }
}
