
import java.io.IOException;
import java.net.Socket;

public class Client {
    protected Connection connection;
    private volatile boolean clientConnected = false;

    protected String getServerAddress(){
        ConsoleHelper.writeMessage("Введите адрес сервера:");
        return ConsoleHelper.readString();
    }

    protected int getServerPort(){
        ConsoleHelper.writeMessage("Введите порт сервера:");
        return ConsoleHelper.readInt();
    }

    protected String getUserName(){
        ConsoleHelper.writeMessage("Введите имя пользователя");
        return ConsoleHelper.readString();
    }

    protected SocketThread getSocketThread(){
        return new SocketThread();
    }

    protected void sendTextMessage(String text){
        try {
            connection.send(new Message(MessageType.TEXT, text));
        } catch (IOException e){
            ConsoleHelper.writeMessage("Не удалось отправить сообщение");
            clientConnected = false;
        }
    }
    protected boolean shouldSendTextFromConsole() {
        return true;
    }

    //ConsoleHelper выводит текст/сообщение/что угодно на консоль

    public class SocketThread extends Thread {

        @Override
        public void run() {
            try {
                //Запрашиваем адрес и порт сервера
                connection = new Connection(new Socket(getServerAddress(), getServerPort()));

                clientHandshake();
                clientMainLoop();
            } catch (IOException | ClassNotFoundException e){
                notifyConnectionStatusChanged(false);
            }
        }

        protected void clientHandshake() throws IOException, ClassNotFoundException{
            while (true){
                Message message = connection.receive();

                if(message.getType() == MessageType.NAME_REQUEST){
                    //Запрос имени с консоли
                    String name = getUserName();

                    connection.send(new Message(MessageType.USER_NAME, name));
                } else if (message.getType() == MessageType.NAME_ACCEPTED){
                    /*
                    принимаем имя пользователя и сообщаем главному потоку
                     */
                    notifyConnectionStatusChanged(true);
                    return;
                } else {
                    throw new IOException("Unexpected MessageType");
                }
            }
        }

        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            while (true){
                Message message = connection.receive();

                if(message.getType() == MessageType.TEXT){
                    processIncomingMessage(message.getData());
                } else if (message.getType() == MessageType.USER_ADDED){
                    informAboutAddingNewUser(message.getData());
                } else if (message.getType() == MessageType.USER_REMOVED){
                    informAboutDeletingNewUser(message.getData());
                } else {
                    throw new IOException("Unexpected MessageType");
                }

            }
        }

        protected void processIncomingMessage(String message){
            ConsoleHelper.writeMessage(message);
        }

        protected void informAboutAddingNewUser(String userName){
            ConsoleHelper.writeMessage("Участник '" + userName + "' присоединился к чату.");
        }

        protected void informAboutDeletingNewUser(String userName){
            ConsoleHelper.writeMessage("Участник '" + userName + "' покинул чат.");
        }

        protected void notifyConnectionStatusChanged(boolean clientConnected){
            /* ставит состояние клиент коннектит в состояние передоное параметром
            и запускает главный поток клиент
             */
            Client.this.clientConnected = clientConnected;
            synchronized (Client.this){
                Client.this.notify();
            }
        }
    }

    public void run(){
        //Создается отдельный поток для установки соденинения, слушать сообщения сервера
        SocketThread socketThread = getSocketThread();
        socketThread.setDaemon(true); //демон служебный
        socketThread.start();
        try {
            synchronized (this) {
                wait();
            }
        } catch (InterruptedException e) {
            ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента.");
            return;
        }

        if(clientConnected) {
            ConsoleHelper.writeMessage("Соединение установлено. Для выхода наберите команду 'exit'.");
        } else {
            ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента");
            return;
        }

        while (clientConnected){
            String text = ConsoleHelper.readString();

            if ("exit".equalsIgnoreCase(text)){
                break;
            }

            if(shouldSendTextFromConsole()){
                sendTextMessage(text);
            }
        }

    }

    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }
}
