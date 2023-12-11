package client;

import server.ServerWindow;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ClientWindow extends JFrame {

    private static final int WIDTH = 400;
    private static final int HEIGHT = 300;
    private final ServerWindow server;

    private JTextField serverIp;
    private JTextField serverPort;
    private JTextField nickname;
    private JTextField password;
    private JButton loginBtn;
    private JTextArea textArea;
    private JTextField inputMsgArea;
    private JButton sendBtn;
    private boolean isConnected;

    public ClientWindow(ServerWindow server) {
        this.server = server;
        this.isConnected = false;

        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);
        setTitle("Client");

        add(createClientPanel());
        setVisible(true);

        // Добавляем слушателя на закрытие окна, чтобы сервер отключал нашего пользователя, если он закрывает свой клиент
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if(isConnected) {
                    disconnectFromServer();
                }
            }
        });
    }

    private JPanel createClientPanel() {
        JPanel clientPanel = new JPanel(new BorderLayout());

        JPanel loginPanel = createLoginPanel();
        textArea = new JTextArea();
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(textArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        JPanel messagePanel = createMessagePanel();

        clientPanel.add(loginPanel, BorderLayout.NORTH);
        clientPanel.add(scroll);
        clientPanel.add(messagePanel, BorderLayout.SOUTH);

        return clientPanel;
    }
    private JPanel createLoginPanel() {
        JPanel loginPanel = new JPanel(new GridLayout(2,3));

        serverIp = new JTextField("127.0.0.1");
        serverPort = new JTextField("8189");
        nickname = new JTextField("sh1zgara");
        password = new JPasswordField("root");
        loginBtn = new JButton("Connect");

        loginPanel.add(serverIp);
        loginPanel.add(serverPort);
        loginPanel.add(nickname);
        loginPanel.add(password);
        loginPanel.add(loginBtn);

        loginBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isConnected) {
                    connectToServer();
                } else {
                    disconnectFromServer();
                }
            }
        });

        return loginPanel;
    }
    private JPanel createMessagePanel() {
        JPanel messagePanel = new JPanel(new BorderLayout());

        inputMsgArea = new JTextField();
        sendBtn = new JButton("Send");

        messagePanel.add(inputMsgArea);
        messagePanel.add(sendBtn, BorderLayout.EAST);

        sendBtn.addActionListener(e -> sendMessage());
        inputMsgArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                }
            }
        });

        return messagePanel;
    }
    private void sendMessage() {
        if (isConnected) {
            String textMsg = inputMsgArea.getText().trim();
            if (textMsg.equals("")) return;
            inputMsgArea.setText("");
            server.getUpdate(this, textMsg);
        }
    }
    private void connectToServer() {
        textArea.append("Идет подключение к серверу\n");
        if (server.getStatus()) {
            textArea.append("Подключение успешно.\n");
            server.addNewUser(this);
            loginBtn.setText("Disconnect");
            isConnected = true;
        } else {
            textArea.append("Сервер недоступен\n");
        }
    }
    private void disconnectFromServer() {
        textArea.append("Отключение от сервера\n");
        server.removeUser(this);
        loginBtn.setText("Connect");
        isConnected = false;
    }
    public void criticalDisconnect() {
        textArea.append("Связь с сервером пропала\n");
        isConnected = false;
        loginBtn.setText("Connect");
    }
    public String getUserName() {
        return nickname.getText();
    }
    public void printMessage(String textMsg) {
        textArea.append(textMsg);
    }

}
