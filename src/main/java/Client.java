import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client extends JFrame {

    private static final String SERVER_HOST = "127.0.0.1";
    private static final int SERVER_PORT = 3443;
    private Socket clientSocket;
    private Scanner inMessage;
    private PrintWriter outMessage;
    private final JTextField message;
    private final JTextField name;
    private final JTextField ip;
    private final JTextField port;
    private final JTextField password;
    private final JTextArea textAreaMessage;
    private boolean ping = false;
    private boolean loginServer = false;
    private boolean pressLogin = false;
    private boolean pressRegister = false;
    private boolean connectedServer = false;

    private synchronized void setConnectedServer(boolean connectedServer) {
        this.connectedServer = connectedServer;
    }

    private synchronized void setLoginServer(boolean loginServer) {
        this.loginServer = loginServer;
    }

    private synchronized void setPing(boolean ping) {
        this.ping = ping;
    }

    public Client() {

        // Создание элементов и размещение на форме
        setBounds(600, 300, 600, 500);
        setTitle("Клиент");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        textAreaMessage = new JTextArea();
        textAreaMessage.setEditable(false);
        textAreaMessage.setLineWrap(true);

        JScrollPane jsp = new JScrollPane(textAreaMessage);
        add(jsp, BorderLayout.CENTER);

        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.X_AXIS));
        northPanel.setBackground(Color.WHITE);

        add(northPanel, BorderLayout.NORTH);

        ip = new JTextField();
        ip.setText(SERVER_HOST);
        ip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                        "Адрес сервера"),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));
        northPanel.add(ip);

        port = new JTextField();
        port.setText("" + SERVER_PORT);
        port.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                        "Порт"),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));
        northPanel.add(port);

        name = new JTextField("");
        name.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                        "Имя"),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));
        northPanel.add(name);

        password = new JTextField("");
        password.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                        "Пароль"),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));
        northPanel.add(password);

        JButton jbLogin = new JButton("Вход");
        northPanel.add(jbLogin);

        JButton jbRegister = new JButton("Регистрация");
        northPanel.add(jbRegister);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        add(bottomPanel, BorderLayout.SOUTH);

        JButton jbSendMessage = new JButton("Отправить");
        bottomPanel.add(jbSendMessage, BorderLayout.EAST);

        message = new JTextField("");
        message.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                        "Сообщение"),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));
        bottomPanel.add(message, BorderLayout.CENTER);

        //событие кнопки Отправить
        jbSendMessage.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!message.getText().equals("") && loginServer) {
                    sendMsg();
                    message.grabFocus();
                }
            }
        });

        //событие кнопки Вход
        jbLogin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                if (checkField())
                    pressLogin = true;
            }
        });

        //событие кнопки Регистрация
        jbRegister.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                if (checkField())
                    pressRegister = true;
            }
        });

        //событие закрытия окна клиентского приложения
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                try {
                    if (connectedServer) {
                        outMessage.println(name.getText() + " вышел из чата");
                        outMessage.println("logout");
                        outMessage.flush();
                        outMessage.close();
                        inMessage.close();
                        clientSocket.close();
                    }
                } catch (IOException exc) {
                    System.out.println("работа завершена");
                }
            }
        });

        // отображаем форму
        setVisible(true);
        name.setEnabled(false);
        password.setEnabled(false);
        jbLogin.setEnabled(false);
        jbRegister.setEnabled(false);

        //раз в секунду проверяем состояние соединения с сервером
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (!ping) {
                        setLoginServer(false);
                        setConnectedServer(false);
                        name.setEnabled(false);
                        password.setEnabled(false);
                        jbLogin.setEnabled(false);
                        jbRegister.setEnabled(false);
                        ip.setEnabled(true);
                        port.setEnabled(true);
                    }
                    setPing(false);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

// работа с сервером
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        if (!connectedServer) {
                            try {
                                // пробуем подключиться к серверу
                                clientSocket = new Socket(ip.getText(), Integer.parseInt(port.getText()));
                                inMessage = new Scanner(clientSocket.getInputStream());
                                outMessage = new PrintWriter(clientSocket.getOutputStream());
                                setConnectedServer(true);
                                textAreaMessage.append("Соединение с сервером установлено (Войдите или Зарегистрируйтесь)\n");
                                ip.setEnabled(false);
                                port.setEnabled(false);
                                name.setEnabled(true);
                                password.setEnabled(true);
                                jbLogin.setEnabled(true);
                                jbRegister.setEnabled(true);

                            } catch (IOException error) {
                                textAreaMessage.append("Нет соединения с сервером\n");
                            }
                        } else {
                            // Соединение установлено
                            // Логинимся
                            if (pressLogin) {
                                outMessage.println(name.getText() + ":" + password.getText() + ":Login");
                                outMessage.flush();
                                pressLogin = false;
                            }
                            // или регистрируемся
                            if (pressRegister) {
                                outMessage.println(name.getText() + ":" + password.getText() + ":Register");
                                outMessage.flush();
                                pressRegister = false;
                            }
                            // если есть входящее сообщение
                            if (inMessage.hasNext()) {
                                // считываем его
                                String inMes = inMessage.nextLine();

                                //если пришло сообщение о успешном логине или регистрации
                                if (inMes.equals("login ok") || inMes.equals("register ok")) {
                                    setLoginServer(true);
                                    textAreaMessage.setText("");
                                    name.setEnabled(false);
                                    password.setEnabled(false);
                                    jbLogin.setEnabled(false);
                                    jbRegister.setEnabled(false);
                                }
                                // сервер прислал пинг
                                if (inMes.equals("Ping")) {
                                    setPing(true);
                                }
                                //отображение сообщений сервера
                                if ((loginServer || inMes.equals("неправильный логин или пароль") || inMes.equals("логин уже занят")) && !inMes.equals("Ping")) {
                                    textAreaMessage.append(inMes);
                                    textAreaMessage.append("\n");
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("error");
                }
            }
        }).start();
    }

    // отправка сообщения
    public void sendMsg() {
        String messageStr = name.getText() + ": " + message.getText();
        outMessage.println(messageStr);
        outMessage.flush();
        message.setText("");
    }

    // проверка заполнения полей имя и пароль
    private boolean checkField() {
        if (name.getText().equals("") || password.getText().equals("")) {
            textAreaMessage.append("Поле имя или пароль не может быть пустым!\n");
            return false;
        } else
            return true;
    }
}
