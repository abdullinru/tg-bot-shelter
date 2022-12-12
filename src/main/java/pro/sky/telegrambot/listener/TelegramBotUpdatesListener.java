package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.constant.Keyboard;
import pro.sky.telegrambot.service.InfoPetsService;
import pro.sky.telegrambot.service.InfoShelterService;
import pro.sky.telegrambot.service.KeyboardService;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private final Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);
    private final KeyboardService keyboardService;
    private final InfoShelterService infoShelterService;
    private final InfoPetsService infoPetsService;
    @Autowired
    private TelegramBot telegramBot;
    public TelegramBotUpdatesListener(KeyboardService keyboardService, InfoShelterService infoShelterService, InfoPetsService infoPetsService) {
        this.keyboardService = keyboardService;
        this.infoShelterService = infoShelterService;
        this.infoPetsService = infoPetsService;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            logger.info("Processing update: {}", update);
            if (update.message() != null) {
                String messageText = update.message().text();
                if (update.message().text().equals(Keyboard.START.getCommand())) {
                    Long chatId = update.message().chat().id();
                    String msgText = "Привет друг! Выбери приют";
                    InlineKeyboardMarkup inlineKeyboard = keyboardService.prepareKeyboardStart();
                    keyboardService.responseOnCommand(chatId, msgText, inlineKeyboard);
                }
            }
            else if (update.callbackQuery() != null) {
                String callbackQuery = update.callbackQuery().data();
                Long chatId = update.callbackQuery().message().chat().id();
                String messageText = null;
                ////////////////////////////////
                // кнопки после команды старт

                if (callbackQuery.equals(Keyboard.CAT.getCommand())) {
                    String msgText = "Меню приюта кошек";
                    InlineKeyboardMarkup inlineKeyboard = keyboardService.prepareKeyboardCat();
                    keyboardService.responseOnCommand(chatId, msgText, inlineKeyboard);
                }
                if (callbackQuery.equals(Keyboard.DOG.getCommand())) {
                    String msgText = "Меню приюта собак";
                    InlineKeyboardMarkup inlineKeyboard = keyboardService.prepareKeyboardDog();
                    keyboardService.responseOnCommand(chatId, msgText, inlineKeyboard);
                }
                ////////////////////////////////////
                // кнопки после команды DOG
                ///////////////////////////////////////////////
                if (callbackQuery.equals(Keyboard.ONE_DOG.getCommand())) {
                    String msgText = "Что тебя интересует о приюте собак";
                    InlineKeyboardMarkup inlineKeyboard = keyboardService.prepareKeyboardInfoShelterDog();
                    keyboardService.responseOnCommand(chatId, msgText, inlineKeyboard);
                }
                if (callbackQuery.equals(Keyboard.TWO_DOG.getCommand())) {
                    String msgText = "Как взять собаку из приюта";
                    InlineKeyboardMarkup inlineKeyboard = keyboardService.prepareKeyboardInfoPetsDog();
                    keyboardService.responseOnCommand(chatId, msgText, inlineKeyboard);
                }
                if (callbackQuery.equals(Keyboard.THREE_DOG.getCommand())) {
                    String msgText = "Информация по отправке отчета";
                    InlineKeyboardMarkup inlineKeyboard = keyboardService.prepareKeyboardReportDog();
                    keyboardService.responseOnCommand(chatId, msgText, inlineKeyboard);
                }
                if (callbackQuery.equals(Keyboard.FOUR_DOG.getCommand())) {
                    keyboardService.responseOnCommandCallVolunteerDog(chatId);
                }

                ////////////////////////////////////
                // кнопки после команды CAT
                if (callbackQuery.equals(Keyboard.ONE_CAT.getCommand())) {
                    String msgText = "Что тебя интересует о приюте кошек";
                    InlineKeyboardMarkup inlineKeyboard = keyboardService.prepareKeyboardInfoShelterCat();
                    keyboardService.responseOnCommand(chatId, msgText, inlineKeyboard);
                }
                if (callbackQuery.equals(Keyboard.TWO_CAT.getCommand())) {
                    String msgText = "Как взять кошку из приюта";
                    InlineKeyboardMarkup inlineKeyboard = keyboardService.prepareKeyboardInfoPetsCat();
                    keyboardService.responseOnCommand(chatId, msgText, inlineKeyboard);
                }
                if (callbackQuery.equals(Keyboard.THREE_CAT.getCommand())) {
                    String msgText = "Информация по отправке отчета";
                    InlineKeyboardMarkup inlineKeyboard = keyboardService.prepareKeyboardReportCat();
                    keyboardService.responseOnCommand(chatId, msgText, inlineKeyboard);
                }
                if (callbackQuery.equals(Keyboard.FOUR_CAT.getCommand())) {
                    keyboardService.responseOnCommandCallVolunteerCat(chatId);
                }

                ////////////////////////////////////////////////////
                // кнопки после команды DOG инфо о приюте
                if (callbackQuery.equals(Keyboard.info_shelter_dog.getCommand())) {
                    messageText = infoShelterService.sendInfoShelter(callbackQuery);
                }
                if (callbackQuery.equals(Keyboard.work_time_and_address_dog.getCommand())) {
                    messageText = infoShelterService.sendWorkTimeAddressMap(callbackQuery);
                }
                if (callbackQuery.equals(Keyboard.shelter_rules_dog.getCommand())) {
                    messageText = infoShelterService.sendShelterRules(callbackQuery);
                }
                if (callbackQuery.equals(Keyboard.security_contacts_dog.getCommand())) {
                    messageText = infoShelterService.sendSecurityContacts(callbackQuery);
                }
                if (callbackQuery.equals(Keyboard.safety_precautions_dog.getCommand())) {
                    messageText = infoShelterService.sendSafetyPrecautions(callbackQuery);
                }
                ////////////////////////////////////
                // кнопки после команды CAT инфо о приюте
                if (callbackQuery.equals(Keyboard.info_shelter_cat.getCommand())){
                    messageText = infoShelterService.sendInfoShelter(callbackQuery);
                }
                if (callbackQuery.equals(Keyboard.work_time_and_address_cat.getCommand())){
                    messageText = infoShelterService.sendWorkTimeAddressMap(callbackQuery);
                }
                if (callbackQuery.equals(Keyboard.shelter_rules_cat.getCommand())){
                    messageText = infoShelterService.sendShelterRules(callbackQuery);
                }
                if (callbackQuery.equals(Keyboard.security_contacts_cat.getCommand())){
                    messageText = infoShelterService.sendSecurityContacts(callbackQuery);
                }
                if (callbackQuery.equals(Keyboard.safety_precautions_cat.getCommand())){
                    messageText = infoShelterService.sendSafetyPrecautions(callbackQuery);
                }
                ////////////////////////////////
                if (callbackQuery.equals(Keyboard.DATING_RULES_DOG.getCommand())){
                    messageText = infoPetsService.datingRules(callbackQuery);
                }
                if (callbackQuery.equals(Keyboard.DOCUMENTS_LIST_DOG.getCommand())){
                    messageText = infoPetsService.documentsList(callbackQuery);
                }
                if (callbackQuery.equals(Keyboard.TRANSPORT_RECOMMENDATIONS_DOG.getCommand())){
                    messageText = infoPetsService.transportRecommendation(callbackQuery);
                }
                if (callbackQuery.equals(Keyboard.HOME_IMPROVEMENT_PUPPY_DOG.getCommand())){
                    messageText = infoPetsService.homeImprovementPuppyOrKitten(callbackQuery);
                }
                if (callbackQuery.equals(Keyboard.HOME_IMPROVEMENT_DOG.getCommand())){
                    messageText = infoPetsService.homeImprovementDogOrCat(callbackQuery);
                }
                if (callbackQuery.equals(Keyboard.HOME_IMPROVEMENT_DISABLED_DOG.getCommand())){
                    messageText = infoPetsService.homeImprovementDisabledDog(callbackQuery);
                }
                if (callbackQuery.equals(Keyboard.CYNOLOGIST_TIPS_DOG.getCommand())){
                    messageText = infoPetsService.cynolistTips(callbackQuery);
                }
                if (callbackQuery.equals(Keyboard.CYNOLOGISTS_LIST_DOG.getCommand())){
                    messageText = infoPetsService.cynolistList(callbackQuery);
                }
                if (callbackQuery.equals(Keyboard.BOUNCE_LIST_DOG.getCommand())){
                    messageText = infoPetsService.bounceList(callbackQuery);
                }

                ///////////////
                if (callbackQuery.equals(Keyboard.DATING_RULES_CAT.getCommand())){
                    messageText = infoPetsService.datingRules(callbackQuery);
                }
                if (callbackQuery.equals(Keyboard.DOCUMENTS_LIST_CAT.getCommand())){
                    messageText = infoPetsService.documentsList(callbackQuery);
                }
                if (callbackQuery.equals(Keyboard.TRANSPORT_RECOMMENDATIONS_CAT.getCommand())){
                    messageText = infoPetsService.transportRecommendation(callbackQuery);
                }
                if (callbackQuery.equals(Keyboard.HOME_IMPROVEMENT_PUPPY_CAT.getCommand())){
                    messageText = infoPetsService.homeImprovementPuppyOrKitten(callbackQuery);
                }
                if (callbackQuery.equals(Keyboard.HOME_IMPROVEMENT_CAT.getCommand())){
                    messageText = infoPetsService.homeImprovementDogOrCat(callbackQuery);
                }
                if (callbackQuery.equals(Keyboard.HOME_IMPROVEMENT_DISABLED_CAT.getCommand())){
                    messageText = infoPetsService.homeImprovementDisabledDog(callbackQuery);
                }
                if (callbackQuery.equals(Keyboard.BOUNCE_LIST_CAT.getCommand())){
                    messageText = infoPetsService.bounceList(callbackQuery);
                }
                if (messageText != null){
                    SendMessage message = new SendMessage(chatId, messageText);
                    telegramBot.execute(message);
                }
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

}
