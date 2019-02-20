package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    public static void sendBroadcastMessage(Message message) {
        try {
            for (Map.Entry<String, Connection> entry : connectionMap.entrySet()) {
                Connection connection = entry.getValue();
                connection.send(message);
            }
        }
        catch (Exception e) {
            System.out.println("Сообщение отправить не получилось.");
        }
    }


    public static void main(String[] args) throws IOException {
        int port =  ConsoleHelper.readInt();
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Сервер запущен.");

            while (true) {
                Socket connection = serverSocket.accept();
                Handler handler = new Handler(connection);
                handler.start();
            }
        } catch (Exception e) {
            System.out.println("Произошла ошибка.");
        }
        finally {
            serverSocket.close();
        }

    }

    private static class Handler extends Thread {
        private Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                System.out.println(socket.getRemoteSocketAddress());
                Connection connection = new Connection(socket);
                String userName = serverHandshake(connection);
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, userName));
                sendListOfUsers(connection, userName);
                serverMainLoop(connection, userName);
                connectionMap.remove(userName);
                sendBroadcastMessage(new Message(MessageType.USER_REMOVED, userName));
            } catch (IOException e) {
                e.printStackTrace();
            }

            catch (ClassNotFoundException ex) {
                ex.printStackTrace();
            }
        }

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {
            Message answer;
            while (true) {
                connection.send(new Message(MessageType.NAME_REQUEST));
                answer = connection.receive();
                if (answer.getType() != MessageType.USER_NAME)
                    continue;
                if (answer.getData().length() > 0 && !connectionMap.containsKey(answer.getData()))
                    break;
            }

            connectionMap.put(answer.getData(), connection);
            connection.send(new Message(MessageType.NAME_ACCEPTED));

            return answer.getData();
        }

        private void sendListOfUsers(Connection connection, String userName) throws IOException {
            for (Map.Entry<String, Connection> entry : connectionMap.entrySet()) {
                if (!entry.getKey().equals(userName))
                    connection.send(new Message(MessageType.USER_ADDED, entry.getKey()));
            }
        }

        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receive();
                if (message.getType() == MessageType.TEXT) {
                    Message broadcastMessage = new Message(MessageType.TEXT, userName + ": " + message.getData());
                    sendBroadcastMessage(broadcastMessage);
                }
                else
                    ConsoleHelper.writeMessage("Ошибка!");
            }
        }
    }
}
