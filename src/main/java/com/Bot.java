import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


enum State {
    START, RELOAD, INP_NUM, INP_DATE, UNKNOWN
}

public class Bot extends TelegramLongPollingBot {


    private State state = State.UNKNOWN;
    private Double amount = 0.0;
    private String date = "";
    private InlineKeyboardMarkup markup;
    private String message_text = "";
    private long chat_id;
    private String reply_text = "";

    Bot() {
        state = State.START;
    }

    private JsonObject getCurrenry(String str_url, String json_el) {
        //Подключаемся к сайту с котировками
        URL url = null;
        HttpURLConnection request = null;

        try {
            url = new URL(str_url);
            request = (HttpURLConnection) url.openConnection();
            request.connect();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Convert to a JSON object to print data
        JsonParser jp = new JsonParser(); //from json
        JsonElement root = null; //Convert the input stream to a json element
        try {
            root = jp.parse(new InputStreamReader((InputStream) request.getContent()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        JsonObject rootobj = root.getAsJsonObject(); //May be an array, may be an object.
        request.disconnect();

        return rootobj.getAsJsonObject(json_el);

    }

    @Override
    public void onUpdateReceived(Update update) {

        String todayPriceURL = "https://blockchain.info/ru/ticker"; //Текущий курс
        String oldPriceURL = "http://api.coindesk.com/v1/bpi/historical/close.json"; //Исторический курс


        // We check if the update has a message and the message has text
        if (update.hasMessage() && update.getMessage().hasText() || update.hasCallbackQuery()) {
            // Set variables

            if (!update.hasCallbackQuery()) {
                message_text = update.getMessage().getText();
                chat_id = update.getMessage().getChatId();
                if(message_text.equals("/start")) state = State.START;
            }

            markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            rowInline.add(new InlineKeyboardButton().setText("ХОЧУ!!!").setCallbackData("update_msg_text"));
            // Set the keyboard to the markup
            rowsInline.add(rowInline);
            // Add it to the message

            switch (state) {
                case START:
                    reply_text = "Привет.\nСейчас ты узнаешь как сильно ты ошибся, не купив криптовалюту. Всё будем считать в деревянных, чтобы тебе было проще. Поехали?";
                    markup.setKeyboard(rowsInline);
                    state = State.RELOAD;
                    break;
                case RELOAD:
                    reply_text = "Такс, ну и сколько ты мог вложить рубасов в биткоин?";
                    state = State.INP_NUM;
                    break;
                case INP_NUM:
                    if (message_text.matches("-?\\d+(\\.\\d+)?")) {
                        //Take the currency at certain date
                        amount = Double.parseDouble(message_text);
                        reply_text = "И когда же ты это мог сделать? \nВ формате YYYY-MM-DD, пожалуйста.";
                        state = State.INP_DATE;
                    } else {
                        reply_text = "Я ТЕБЕ СКАЗАЛ В ФОРМАТЕ YYYY-MM-DD!!!";
                    }
                    break;
                case INP_DATE:

                    date = message_text;
                    Double todayCurrency = getCurrenry(todayPriceURL, "RUB").get("last").getAsDouble(); //just grab the last currency
                    Double oldCurrency = getCurrenry(oldPriceURL + "?start=" + date + "&end=" + date + "&currency=RUB", "bpi").get(date).getAsDouble(); //just grab the last currency

                    double result = amount / oldCurrency * todayCurrency - amount;

                    reply_text = "Ты, мог заработать " + String.valueOf(Math.round(result)) + " руб. чистой прибыли, если бы вложил "
                            + amount + " руб. в биткоин в " + date.subSequence(0, 4) + "-ом году. Нужно было верить в блокчейн, а не бояться МММ. \nХочешь еще раз?";
                    markup.setKeyboard(rowsInline);
                    state = State.RELOAD;
                    break;
                default:
                    reply_text = "";
                    break;

            /* Кнопки для инициализации календаря
            CalendarUtil calendar = new CalendarUtil();
            LocalDate d = LocalDate.now();
            InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
            markupInline.setKeyboard(calendar.generateKeyboard(d));
            message.setReplyMarkup(markupInline);*/

            }

            sendMsg(chat_id, reply_text, markup);

        }

    }


    @Override
    public String getBotUsername() {
        // Return bot username
        // If bot username is @MyAmazingBot, it must return 'Bot'
        return "BTC dreamer";
    }

    @Override
    public String getBotToken() {
        // Return bot token from BotFather
        return "420072940:AAFAZGj0n4nZk3YUJwKYH57EWnW197QVTRw";
    }

    @SuppressWarnings("deprecation") // Означает то, что в новых версиях метод уберут или заменят
    public void sendMsg(long id, String text, InlineKeyboardMarkup markup) {
        SendMessage message = new SendMessage() // Create a message object object
                .setChatId(id)
                .setText(text)
                .setReplyMarkup(markup);
        try {
            execute(message); // Sending our message object to user
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
