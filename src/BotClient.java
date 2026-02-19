import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class BotClient extends Client {
    @Override
    protected SocketThread getSocketThread() {
        return new BotSocketThread();
    }

    @Override
    protected boolean shouldSendTextFromConsole() {
        // Бот не должен отправлять текст введенный с консоли
        return false;
    }

    @Override
    protected String getUserName() {
        // Каждый раз генерируем новое имя бота на случай, если к серверу подключится несколько ботов
        return "date_bot_" + (int) (Math.random() * 100);
    }

    public static void main(String[] args) {
        // Создаем и запускаем бот клиента
        Client client = new BotClient();
        client.run();
    }

    public class BotSocketThread extends SocketThread {
        @Override
        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            sendTextMessage("Привет чатику. Я бот. Понимаю команды: дата, день, месяц, год, время, час, минуты, секунды.");
            super.clientMainLoop();
        }

        @Override
        protected void processIncomingMessage(String message) {
            ConsoleHelper.writeMessage(message);

            //отделяем отправителя от его текста
            String userNameDelimiter = ": ";
            String[] split = message.split(userNameDelimiter);
            if(split.length != 2) {
                return;
            }
            String messageWithoutUserName = split[1];

            //Готовим формат согласно запросу
            String format = null;
            switch (messageWithoutUserName){
                case "дата":
                    format = "d.MM.YYYY";
                    break;
                case "день":
                    format = "d";
                    break;
                case "месяц":
                    format = "MMMM";
                    break;
                case "год":
                    format = "YYYY";
                    break;
                case "время":
                    format = "H:mm:ss";
                    break;
                case "час":
                    format = "H";
                    break;
                case "минуты":
                    format = "m";
                    break;
                case "секунды":
                    format = "s";
                    break;
            }

            if(format != null){
                String answer = new SimpleDateFormat(format).format(Calendar.getInstance().getTime());
                BotClient.this.sendTextMessage("Информация для " + split[0] + ": " + answer);
            }
        }
    }
}

