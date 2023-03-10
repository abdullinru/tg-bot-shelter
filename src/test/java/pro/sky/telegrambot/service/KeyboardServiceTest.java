package pro.sky.telegrambot.service;

import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import pro.sky.telegrambot.constant.Keyboard;

class KeyboardServiceTest {
    private final KeyboardService keyboardService = new KeyboardService();

    @Test
    void responseOnCommand() {

    }

    @Test
    void prepareKeyboardTest() {
        String[] textButtonsAfterCommand = {
                "Приют для кошек",
                "Приют для собак"
        };
        Keyboard[] keyboards = {
                Keyboard.CAT,
                Keyboard.DOG
        };
        InlineKeyboardMarkup actual = keyboardService.prepareKeyboard(textButtonsAfterCommand, keyboards);
        InlineKeyboardMarkup expected = new InlineKeyboardMarkup();
        InlineKeyboardButton button1 = new InlineKeyboardButton("Приют для кошек");
        InlineKeyboardButton button2 = new InlineKeyboardButton("Приют для собак");
        button1.callbackData(Keyboard.CAT.getCommand());
        button2.callbackData(Keyboard.DOG.getCommand());
        expected.addRow(button1);
        expected.addRow(button2);

        Assertions.assertThat(expected).isEqualTo(actual);

    }
}