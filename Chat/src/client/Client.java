package client;


import server.*;

import java.io.IOException;
import java.net.Socket;


public class Client {
    protected Connection connection;
    private volatile boolean clientConnected = false;

    protected String getServerAddress() throws IOException {
        return ConsoleHelper.readString();
    }

    protected int getServerPort() {
        return ConsoleHelper.readInt();
    }

    protected String getUserName() {
        return ConsoleHelper.readString();
    }

    protected boolean shouldSendTextFromConsole() {
        return true;
    }

    protected SocketThread getSocketThread() {
        return new SocketThread();
    }

    protected void sendTextMessage(String text) {
        try {
            Message message = new Message(MessageType.TEXT, text);
            connection.send(message);
        } catch (IOException e) {
            System.out.println("Возникла ошибка.");
            clientConnected = false;
        }
    }

    public void run() {
        SocketThread socketThread = getSocketThread();
        socketThread.setDaemon(true);
        socketThread.start();

        try {
            synchronized (this) {
                wait();
            }
        } catch (InterruptedException e) {
            ConsoleHelper.writeMessage("Программа завершила работу аварийно " + e.getMessage());
        }

        if (clientConnected) {
            System.out.println("Соединение установлено. Для выхода наберите команду 'exit'.");

            while (clientConnected) {
                String text = ConsoleHelper.readString();

                if (text.equalsIgnoreCase("exit"))
                    break;

                if (shouldSendTextFromConsole())
                    sendTextMessage(text);
            }
        }
        else
            System.out.println("Произошла ошибка во время работы клиента.");
    }


    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }


    public class SocketThread extends Thread {

        @Override
        public void run() {
            try {
                Socket socket = new Socket(getServerAddress(), getServerPort());
                connection = new Connection(socket);
                clientHandshake();
                clientMainLoop();

            } catch (Exception e) {
                notifyConnectionStatusChanged(false);
            }
        }

        protected void processIncomingMessage(String message) {
            ConsoleHelper.writeMessage(message);
        }

        protected void informAboutAddingNewUser(String userName) {
            ConsoleHelper.writeMessage(userName + " присоединился к чату.");
        }

        protected void informAboutDeletingNewUser(String userName) {
            ConsoleHelper.writeMessage(userName + " покинул чат.");
        }

        protected void notifyConnectionStatusChanged(boolean clientConnected) {
            Client.this.clientConnected = clientConnected;

            synchronized(Client.this) {
                Client.this.notify();
            }
        }

        protected void clientHandshake() throws IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receive();
                if (message.getType() == MessageType.NAME_REQUEST) {
                    connection.send(new Message(MessageType.USER_NAME, getUserName()));
                    continue;
                }
                else if (message.getType() == MessageType.NAME_ACCEPTED) {
                    notifyConnectionStatusChanged(true);
                    break;
                }
                else
                    throw new IOException("Unexpected server.MessageType");
            }
        }

        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receive();
                if (message.getType() == MessageType.TEXT) {
                    processIncomingMessage(message.getData());
                }
                else if (message.getType() == MessageType.USER_ADDED) {
                    informAboutAddingNewUser(message.getData());
                }
                else if (message.getType() == MessageType.USER_REMOVED) {
                    informAboutDeletingNewUser(message.getData());
                }
                else
                    throw new IOException("Unexpected server.MessageType");
            }
        }
    }
}
