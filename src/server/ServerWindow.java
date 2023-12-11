package server;

import client.ClientWindow;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ServerWindow extends JFrame {
    private static final int WIDTH = 600;
    private static final int HEIGHT = 300;
    private static final String BACKUP_FILE = "src\\server\\chat_history.txt";
    private static final String LOGS_FILE = "src\\server\\system_logs.txt";
    private static final int HISTORY_MSG_COUNT = 10;
    private boolean isWorking;
    private JButton start;
    private JButton stop;
    private JButton send;
    private JTextArea textArea;
    private JTextField inputMsgArea;
    private DefaultListModel<ClientWindow> list;
    private List<String> history;

    public ServerWindow() {
        this.isWorking = false;
        list = new DefaultListModel<>();
        history = new ArrayList<>();

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);
        setResizable(false);
        setTitle("Chat server");

        Component serverPanel = createServerPanel();
        add(serverPanel);

        setVisible(true);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                textArea.append(String.format("[System][%tT] Принудительное выключение сервера\n", new Date()));
                if(isWorking) {
                    disconnectAllUsers();
                }
                textArea.append(String.format("[System][%tT] Сервер полностью выключен\n", new Date()));
                try {
                    saveHistory();
                } catch (IOException | BadLocationException ex) {
                    throw new RuntimeException("Попытка создания бэкапа не удалась");
                }
            }
        });
    }
    private Component createServerPanel() {
        start = new JButton("Start server");
        stop = new JButton("Stop server");
        send = new JButton("Send");

        textArea = new JTextArea();
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(textArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        inputMsgArea = new JTextField();

        // Часть сервисного компонента сервера для отправки сообщений пользователям от Админа
        JPanel serverMessagePanel = new JPanel(new BorderLayout());
        serverMessagePanel.add(send, BorderLayout.EAST);
        serverMessagePanel.add(inputMsgArea);

        // Часть сервисного компонента сервера, отвечающая за включение/отключение сервера
        JPanel serverControlPanel = new JPanel(new GridLayout(1, 2));
        serverControlPanel.add(start);
        serverControlPanel.add(stop);

        // Сервисный компонент сервера, отвечающий за управление сервером
        JPanel serverServicePanel = new JPanel(new GridLayout(2, 1));
        serverServicePanel.add(serverMessagePanel);
        serverServicePanel.add(serverControlPanel);

        // Итоговый компонент для сервера
        JPanel serverPanel = new JPanel(new BorderLayout());
        serverPanel.add(serverServicePanel, BorderLayout.SOUTH);
        serverPanel.add(scroll);

        start.addActionListener(e -> {
            if (isWorking) {
                textArea.append(String.format("[System][%tT] Сервер уже запущен!\n", new Date()));
            } else {
                isWorking = true;
                textArea.append(String.format("[System][%tT] Сервер запущен!\n", new Date()));
            }
        });
        stop.addActionListener(e -> {
            if (isWorking) {
                textArea.append(String.format("[System][%tT] Ручная остановка сервера!\n", new Date()));
                disconnectAllUsers();
                isWorking = false;
                textArea.append(String.format("[System][%tT] Сервер остановлен!\n", new Date()));
            } else {
                textArea.append(String.format("[System][%tT] Сервер выключен\n", new Date()));
            }
        });
        send.addActionListener(e -> {
            sendAdminMessage();
        });
        inputMsgArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendAdminMessage();
                }
            }
        });

        return serverPanel;
    }
    public boolean getStatus() {
        return this.isWorking;
    }
    public void addNewUser(ClientWindow client) {
        textArea.append(String.format(
                "[System][%tT] Подключился новый пользователь: %s\n", new Date(), client.getUserName()));
        list.addElement(client);
        sendHistory(client);
    }
    public void removeUser(ClientWindow client) {
        list.removeElement(client);
        textArea.append(String.format(
                "[System][%tT] Пользователь: %s отключился от сервера\n", new Date(), client.getUserName()));
    }
    private void disconnectAllUsers() {
        // Честно говоря так и не понял как итерироваться по JList или по вот таким коллекциям
        // Поэтому будем примитивным методом)
        while (list.getSize() > 0) {
            ClientWindow client = list.getElementAt(list.getSize() - 1);
            // А еще не смог разобраться как можно было бы сделать какого либо вызов события, которое ловил бы клиент
            // или изменение какого нибудь состояния окна, чтобы имитировать нештатную ситуацию якобы разрыв соединения
            client.criticalDisconnect();
            textArea.append(String.format("[System][%tT] Пользователь: %s отключен\n", new Date(), client.getUserName()));
            list.removeElement(client);
        }
        isWorking = false;
    }
    public void getUpdate(ClientWindow client, String textMsg) {
        String msgToSend = String.format("[%tT] %s: %s\n", new Date(), client.getUserName(), textMsg);
        textArea.append(msgToSend);
        history.add(msgToSend);
        sendMessagesToUsers(msgToSend);
    }
    private void sendAdminMessage() {
        String msgToSend = String.format("[%tT] NOTICE: %s\n", new Date(), inputMsgArea.getText());
        textArea.append(msgToSend);
        history.add(msgToSend);
        sendMessagesToUsers(msgToSend);
        inputMsgArea.setText("");
    }
    private void sendMessagesToUsers(String msgToSend) {
        for (int i = 0; i < list.getSize(); i++) {
            list.getElementAt(i).printMessage(msgToSend);
        }
    }
    private void sendHistory(ClientWindow client) {
        int historyLength = history.size();
        int countMsgToSend = Math.min(historyLength, HISTORY_MSG_COUNT);

        for (int i = historyLength - countMsgToSend; i < historyLength; i++) {
            client.printMessage(history.get(i));
        }
    }
    private void saveHistory() throws IOException, BadLocationException {
        try (BufferedWriter historyWriter = new BufferedWriter(new FileWriter(BACKUP_FILE, true));
             BufferedWriter logWriter = new BufferedWriter(new FileWriter(LOGS_FILE, true))) {

            int start = 0;
            for(int i = 0; i < textArea.getLineCount(); i++)
            {
                int end = textArea.getLineEndOffset(i);
                String textAreaLine = textArea.getText(start, end - start).replace("\n", "");
                if (textAreaLine.startsWith("[System]")) {
                    logWriter.write(textAreaLine + "\r\n");
                } else {
                    historyWriter.write(textAreaLine + "\r\n");
                }
                start = end;
            }
        }
    }
    private List<String> loadHistory() throws IOException {
        return Files.readAllLines(Paths.get(BACKUP_FILE));
    }
}