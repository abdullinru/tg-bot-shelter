package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.PhotoSize;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.constant.Icon;
import pro.sky.telegrambot.constant.Keyboard;
import pro.sky.telegrambot.model.CatOwner;
import pro.sky.telegrambot.model.DogOwner;
import pro.sky.telegrambot.model.KeepingPet;
import pro.sky.telegrambot.model.User;
import pro.sky.telegrambot.service.*;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static pro.sky.telegrambot.constant.Keyboard.START;
import static pro.sky.telegrambot.constant.KeyboardMenu.*;
import static pro.sky.telegrambot.constant.MessageForDailyReport.*;
import static pro.sky.telegrambot.constant.MessageForSaveContacts.*;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private final Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);
    private final KeyboardService keyboardService;
    private final InfoPetsService infoPetsService;
    private final KeepingPetService keepingPetService;
    private final PetOwnerService petOwnerService;

    private final UserService userService;
    @Autowired
    private TelegramBot telegramBot;
    @Value("${volunteer-chat-id}")
    private Long volunteerChatId;

    public TelegramBotUpdatesListener(KeyboardService keyboardService, InfoPetsService infoPetsService, KeepingPetService keepingPetService, PetOwnerService petOwnerService, UserService userService, TelegramBot telegramBot) {
        this.keyboardService = keyboardService;
        this.infoPetsService = infoPetsService;
        this.keepingPetService = keepingPetService;
        this.petOwnerService = petOwnerService;
        this.userService = userService;
        this.telegramBot = telegramBot;
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

                // ???????????????????????? ???????????????? ??????????????????
                if (update.message().text() != null) {
                    String cmd = update.message().text();
                    if (cmd.equals(START.getCommand())) {
                        Long chatId = update.message().chat().id();
                        String msgText = ("???????????? ????????! " + Icon.WAVE_Icon.get()) +
                                ("\n???????????? ??????????" + Icon.HAND_Icon.get());
                        InlineKeyboardMarkup inlineKeyboard = keyboardService.prepareKeyboard(
                                textButtonsAfterCommandStart,
                                keyboardsAfterCommandStart);
                        keyboardService.responseOnCommand(chatId, msgText, inlineKeyboard);
                    }


                    // ???????????????????????? ?????????????? ???? ?????????????????? ????????
                    else if (update.message().replyToMessage() != null &&
                            !update.message().replyToMessage().text().isEmpty()) {
                        Long chatId = update.message().chat().id();
                        String userRequest = update.message().text();
                        String replyMessage = update.message().replyToMessage().text();

                        // ???????????????? ???????????????????? ????????????
                        if (replyMessage.equals(NAME) || replyMessage.equals(NAME_AGAIN)) {
                            userService.saveContactInfo(chatId, PHONE, userRequest);
                        }
                        if (replyMessage.equals(PHONE) || replyMessage.equals(PHONE_AGAIN)) {
                            userService.saveContactInfo(chatId, MAIL, userRequest);
                        }
                        if (replyMessage.equals(MAIL) || replyMessage.equals(MAIL_AGAIN)) {
                            userService.saveContactInfo(chatId, SAVE, userRequest);
                        }

                        // ???????????????????????? ???????????????? ?? ???????????? ?? ???????????????? ?????????? ?????? ????????
                        if (replyMessage.equals(SEND_REPORT) ||
                                replyMessage.equals(RE_SEND_REPORT)) {
                            keepingPetService.sendReport(chatId, RE_SEND_REPORT);
                        }

                    }
                    //???????????????????????? ???????????????? ???????? ?????? ???????????? ?? ????????????????
                } else if (update.message().photo() != null &&
                        update.message().replyToMessage() != null) {
                    String replyMessage = update.message().replyToMessage().text();
                    // ???????????????? ???????????? ?? ????????????????
                    if (replyMessage.equals(SEND_REPORT) ||
                            replyMessage.equals(RE_SEND_REPORT)) {

                        Long chatId = update.message().chat().id();
                        PhotoSize[] photoSizes = update.message().photo();
                        String capture = update.message().caption();
                        if (capture == null || photoSizes == null) {
                            keepingPetService.sendReport(chatId, RE_SEND_REPORT);
                        } else if (petOwnerService.findCatOwner(chatId) == null && petOwnerService.findDogOwner(chatId) == null) {
                            keepingPetService.sendReportWithoutReply(chatId, USER_IS_NOT_OWNER);
                        } else {
                            try {
                                keepingPetService.sendReport(chatId, capture, photoSizes);
                                keepingPetService.sendMessage(chatId, "?????????? ????????????????, ???????? ???????????? ????????????");
                            } catch (IOException e) {
                                throw new RuntimeException("???????????????? ?? ?????????????????????? ????????");
                            }
                        }
                    }
                }

            } else if (update.callbackQuery() != null) {
                String callbackQuery = update.callbackQuery().data();
                Long chatId = update.callbackQuery().message().chat().id();
                String messageText = null;

                ////////////////////////////////
                // ???????????? ?????????? ?????????????? ??????????
                if (callbackQuery.equals(Keyboard.CAT.getCommand())) {
                    User user = new User();
                    String msgText = ("???????? ???????????? ?????????? " + Icon.CAT_Icon.get());
                    InlineKeyboardMarkup inlineKeyboard = keyboardService.prepareKeyboard(
                            textButtonsAfterCommandCat,
                            keyboardsAfterCommandCat
                    );
                    User prevUser = userService.findUserByChatId(chatId);
                    if (prevUser == null) {
                        user.setChatId(chatId);
                    } else {
                        user = prevUser;
                    }
                    user.setShelter(callbackQuery);
                    userService.saveUser(user);
                    keyboardService.responseOnCommand(chatId, msgText, inlineKeyboard);
                    }
                if (callbackQuery.equals(Keyboard.DOG.getCommand())) {
                    User user = new User();
                    String msgText = ("???????? ???????????? ?????????? " + Icon.DOG_Icon.get());
                    InlineKeyboardMarkup inlineKeyboard = keyboardService.prepareKeyboard(
                            textButtonsAfterCommandDog,
                            keyboardsAfterCommandDog
                    );
                    User prevUser = userService.findUserByChatId(chatId);
                    if (prevUser == null) {
                        user.setChatId(chatId);
                    } else {
                        user = prevUser;
                    }

                    user.setShelter(callbackQuery);
                    userService.saveUser(user);
                    keyboardService.responseOnCommand(chatId, msgText, inlineKeyboard);
                }
                ////////////////////////////////////
                // ???????????? ?????????? ?????????????? DOG
                ///////////////////////////////////////////////
                if (callbackQuery.equals(Keyboard.ONE_DOG.getCommand())) {
                    String msgText = ("???????????????????? ?? ???????????? ?????????? " + Icon.DOG_Icon.get());
                    InlineKeyboardMarkup inlineKeyboard = keyboardService.prepareKeyboard(
                            textButtonsAfterCommandInfoShelter,
                            keyboardsAfterCommandInfoShelter
                    );
                    keyboardService.responseOnCommand(chatId, msgText, inlineKeyboard);
                }
                if (callbackQuery.equals(Keyboard.TWO_DOG.getCommand())) {
                    String msgText = ("?????? ?????????? ???????????? ???? ???????????? " + Icon.DOG_Icon.get());
                    InlineKeyboardMarkup inlineKeyboard = keyboardService.prepareKeyboard(
                            textButtonsAfterCommandInfoPetsDog,
                            keyboardsAfterCommandInfoPetsDog
                    );
                    keyboardService.responseOnCommand(chatId, msgText, inlineKeyboard);
                }
                if (callbackQuery.equals(Keyboard.THREE_DOG.getCommand())) {
                    String msgText = ("???????????????? ???????????? " + Icon.DOG_Icon.get());
                    InlineKeyboardMarkup inlineKeyboard = keyboardService.prepareKeyboard(
                            textButtonsAfterCommandReportDog,
                            keyboardsAfterCommandReportDog);
                    keyboardService.responseOnCommand(chatId, msgText, inlineKeyboard);
                }
//                if (callbackQuery.equals(Keyboard.FOUR_DOG.getCommand())) {
//                    keyboardService.responseOnCommandCallVolunteerDog(chatId);
//                }

                ////////////////////////////////////
                // ???????????? ?????????? ?????????????? CAT
                if (callbackQuery.equals(Keyboard.ONE_CAT.getCommand())) {
                    String msgText = ("???????????????????? ?? ???????????? ?????????? " + Icon.CAT_Icon.get());
                    InlineKeyboardMarkup inlineKeyboard = keyboardService.prepareKeyboard(
                            textButtonsAfterCommandInfoShelter,
                            keyboardsAfterCommandInfoShelter
                    );
                    keyboardService.responseOnCommand(chatId, msgText, inlineKeyboard);
                }
                if (callbackQuery.equals(Keyboard.TWO_CAT.getCommand())) {
                    String msgText = ("?????? ?????????? ?????????? ???? ???????????? " + Icon.CAT_Icon.get());
                    InlineKeyboardMarkup inlineKeyboard = keyboardService.prepareKeyboard(
                            textButtonsAfterCommandInfoPetsCat,
                            keyboardsAfterCommandInfoPetsCat
                    );
                    keyboardService.responseOnCommand(chatId, msgText, inlineKeyboard);
                }
                if (callbackQuery.equals(Keyboard.THREE_CAT.getCommand())) {
                    String msgText = ("???????????????? ???????????? " + Icon.CAT_Icon.get());
                    InlineKeyboardMarkup inlineKeyboard = keyboardService.prepareKeyboard(
                            textButtonsAfterCommandReportCat,
                            keyboardsAfterCommandReportCat
                    );
                    keyboardService.responseOnCommand(chatId, msgText, inlineKeyboard);
                }
//                if (callbackQuery.equals(Keyboard.FOUR_CAT.getCommand())) {
//                    keyboardService.responseOnCommandCallVolunteerCat(chatId);
//                }
                ////////////////////////////////////
                // ???????????? ???????? ?? ????????????
                if (callbackQuery.equals(Keyboard.info_shelter_.getCommand())) {
                    messageText = infoPetsService.getInfoByRequest(callbackQuery+userService.findShelterByChatId(chatId));
                }
                if (callbackQuery.equals(Keyboard.work_time_and_address_.getCommand())) {
                    messageText = infoPetsService.getInfoByRequest(callbackQuery+userService.findShelterByChatId(chatId));
                }
                if (callbackQuery.equals(Keyboard.shelter_rules_.getCommand())) {
                    messageText = infoPetsService.getInfoByRequest(callbackQuery+userService.findShelterByChatId(chatId));
                }
                if (callbackQuery.equals(Keyboard.security_contacts_.getCommand())) {
                    messageText = infoPetsService.getInfoByRequest(callbackQuery+userService.findShelterByChatId(chatId));
                }
                if (callbackQuery.equals(Keyboard.safety_precautions_.getCommand())) {
                    messageText = infoPetsService.getInfoByRequest(callbackQuery+userService.findShelterByChatId(chatId));
                }
                if (callbackQuery.equals(Keyboard.leave_request_.getCommand())) {
                    userService.saveContactInfo(chatId, NAME, null);
                }
                ///////////////
                // ???????????? ?????? ?????????? ???????????????? ???? ????????????
                if (callbackQuery.equals(Keyboard.DATING_RULES_.getCommand())) {
                    messageText = infoPetsService.getInfoByRequest(callbackQuery+userService.findShelterByChatId(chatId));
                }
                if (callbackQuery.equals(Keyboard.DOCUMENTS_LIST_.getCommand())) {
                    messageText = infoPetsService.getInfoByRequest(callbackQuery+userService.findShelterByChatId(chatId));
                }
                if (callbackQuery.equals(Keyboard.TRANSPORT_RECOMMENDATIONS_.getCommand())) {
                    messageText = infoPetsService.getInfoByRequest(callbackQuery+userService.findShelterByChatId(chatId));
                }
                if (callbackQuery.equals(Keyboard.HOME_IMPROVEMENT_PUPPY_.getCommand())) {
                    messageText = infoPetsService.getInfoByRequest(callbackQuery+userService.findShelterByChatId(chatId));
                }
                if (callbackQuery.equals(Keyboard.HOME_IMPROVEMENT_.getCommand())) {
                    messageText = infoPetsService.getInfoByRequest(callbackQuery+userService.findShelterByChatId(chatId));
                }
                if (callbackQuery.equals(Keyboard.HOME_IMPROVEMENT_DISABLED_.getCommand())) {
                    messageText = infoPetsService.getInfoByRequest(callbackQuery+userService.findShelterByChatId(chatId));
                }
                if (callbackQuery.equals(Keyboard.CYNOLOGIST_TIPS_.getCommand())) {
                    messageText = infoPetsService.getInfoByRequest(callbackQuery+userService.findShelterByChatId(chatId));
                }
                if (callbackQuery.equals(Keyboard.CYNOLOGISTS_LIST_.getCommand())) {
                    messageText = infoPetsService.getInfoByRequest(callbackQuery+userService.findShelterByChatId(chatId));
                }
                if (callbackQuery.equals(Keyboard.BOUNCE_LIST_.getCommand())) {
                    messageText = infoPetsService.getInfoByRequest(callbackQuery+userService.findShelterByChatId(chatId));
                }
                if (callbackQuery.equals(Keyboard.call_volunteer_.getCommand())) {
                    messageText = infoPetsService.getInfoByRequest(callbackQuery);
                }
                ////////////////////////////////
                // ???????????? ?????????? ?????????????? DOG ???????????????? ?????????? ?? ??????????????
                if (callbackQuery.equals(Keyboard.DAILY_REPORT_FORM_DOG.getCommand())) {
                    messageText = infoPetsService.getInfoByRequest(callbackQuery);
                }
                if (callbackQuery.equals(Keyboard.SEND_REPORT_DOG.getCommand())) {
                    keepingPetService.sendReport(chatId, SEND_REPORT);
                }

                ////////////////////////////////
                // ???????????? ?????????? ?????????????? CAT ???????????????? ?????????? ?? ??????????????
                if (callbackQuery.equals(Keyboard.DAILY_REPORT_FORM_CAT.getCommand())) {
                    messageText = infoPetsService.getInfoByRequest(callbackQuery);
                }
                if (callbackQuery.equals(Keyboard.SEND_REPORT_CAT.getCommand())) {
                    keepingPetService.sendReport(chatId, SEND_REPORT);
                }

                if (messageText != null) {
                    SendMessage message = new SendMessage(chatId, messageText);
                    telegramBot.execute(message);
                }
                // ?????????? ?????????????? ???? ????????????????????
//            } else{
//                Long chatId = update.message().chat().id();
//                String msgText = "?????????? ?????????????? ???? ????????????????????. \n\n ?????????? ?????????????????? ?? ?????????????? ????????, ?????????? /start";
//                keyboardService.responseOnCommand(chatId, msgText);
            }

        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    @Scheduled(cron = "${interval-in-cron}")
    public void checkKeepingPet(){
        logger.info("?????????? ??????????????");
        List<KeepingPet> lastAllOwner = keepingPetService.lastKeepingPetOwner();
        for (int i = 0; i < lastAllOwner.size(); i++){
            KeepingPet lastKeepingPet = lastAllOwner.get(i);
            LocalDateTime date = LocalDateTime.now();
            Long difference = Duration.between(lastKeepingPet.getDateTime(),date).toHours();
            logger.info("???????????????? ????????????????");
            if(difference >= 24 & difference <= 48){
                SendMessage message = new SendMessage(lastKeepingPet.getChatId(), "????????????????????, ?????????????????? ??????????");
                telegramBot.execute(message);
            }
            else if(difference > 48 & lastKeepingPet.getCatOwner()!= null){
                CatOwner owner = lastKeepingPet.getCatOwner();
                SendMessage message = new SendMessage(volunteerChatId, "?????????????????? ?? ????????????????????????. ?????????? ???? " +owner.getName()+" "+owner.getPhone()+" ???? ?????? ?????????????? 2 ??????.");
                telegramBot.execute(message);
            }
            else if(difference > 48 & lastKeepingPet.getDogOwner()!= null){
                DogOwner owner = lastKeepingPet.getDogOwner();
                SendMessage message = new SendMessage(volunteerChatId, "?????????????????? ?? ????????????????????????. ?????????? ???? " +owner.getName()+" "+owner.getPhone()+" ???? ?????? ?????????????? 2 ??????.");
                telegramBot.execute(message);
            }
        }
    }

}
