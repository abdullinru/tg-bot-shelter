package pro.sky.telegrambot.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.File;
import com.pengrad.telegrambot.model.PhotoSize;
import com.pengrad.telegrambot.model.request.ForceReply;
import com.pengrad.telegrambot.request.GetFile;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.GetFileResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.CatOwner;
import pro.sky.telegrambot.model.DogOwner;
import pro.sky.telegrambot.model.KeepingPet;
import pro.sky.telegrambot.model.PhotoPet;
import pro.sky.telegrambot.repositories.KeepingPetRepository;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardOpenOption.CREATE_NEW;

/**
 * Сервис, описывающий методы по ведению питомца хозяевами
 */
@Service
public class KeepingPetService {

    private final PetOwnerService petOwnerService;
    private final PhotoPetService photoPetService;
    private final KeepingPetRepository keepingPetRepository;

    @Autowired
    private TelegramBot telegramBot;
    private final String coversDir = "C://Users//Lu//Desktop//обучение";

    public KeepingPetService(PetOwnerService petOwnerService, PhotoPetService photoPetService, KeepingPetRepository keepingPetRepository) {
        this.petOwnerService = petOwnerService;
        this.photoPetService = photoPetService;
        this.keepingPetRepository = keepingPetRepository;
    }

    /**
     * Метод отправляет ежедневный отчет усыновителя, включающиий  фото питомца, рацион, самочувствие, поведение. Отчет сохраняется в БД в таблице KeepingPet
     *
     * @param chatId     идентификатор чата, не может быть null
     * @param photoSizes объект, хранящий информацию с фотографией питомца. не null
     * @param caption    сообщение, отправленное вместе с фото
     * @return KeepingPet (объект инкапсулирующий отчет пользователя)
     */
    public KeepingPet sendReport(Long chatId, String caption, PhotoSize[] photoSizes) throws IOException {
        KeepingPet keepingPet = null;
        try {
            keepingPet = getNewReport(chatId, photoSizes, caption);
        } catch (IllegalArgumentException e) { // владелей не найден по айди или чат айди

        }

        return keepingPetRepository.save(keepingPet);
    }

    /**
     * Метод создает объекты типа PhotoPet и KeepingPet
     * Отправляет эти объекты в базу данных
     * Сохраняет фотографию питомца на сервер в папку
     * @param chatId Идентификатор чата
     * @param photoSizes массив объектов класса PhotoSize (фотографии, отправленные пользователем)
     * @param caption Текстовое описание к фотографии
     * @return KeepingPet сохраненный в БД отчет
     * @throws IOException
     */
    private KeepingPet getNewReport(Long chatId, PhotoSize[] photoSizes, String caption) throws IOException {
        PhotoSize photo = photoSizes[1];
        String fileId = photo.fileId();

        GetFile fileRequest = new GetFile(fileId);
        GetFileResponse fileResponse = telegramBot.execute(fileRequest);
        File file = fileResponse.file();
        byte[] fileData = telegramBot.getFileContent(file);

        Path filePath = Path.of(coversDir, fileId + "." +getExtension(file.filePath()));
        Files.createDirectories(filePath.getParent());
        Files.deleteIfExists(filePath);

        long reportId = findReportIdByChatId(chatId);

        PhotoPet photoPet;
        if (reportId == -1) { // пользователь сегодня еще не отправлял отчет
            photoPet = createPhotoPet(chatId, fileRequest, file, filePath);
            photoPetService.savePhotoReport(photoPet);
            uploadPhotoToServer(fileData, filePath);

            return createKeepingPet(chatId, caption, photoPet);
        }
        else { // пользователь сегодня уже отправлял отчет
            PhotoPet deletePhotoPet = keepingPetRepository.findKeepingPetById(reportId).getPhotoPet();
            Files.deleteIfExists(Path.of(deletePhotoPet.getFilePath()));

            photoPet = updatePhotoPet(reportId, fileRequest, file, filePath);
            photoPetService.savePhotoReport(photoPet);
            uploadPhotoToServer(fileData, filePath);

            return updateKeepingPet(reportId, caption, photoPet);
        }
    }

    /**
     * Метод для поиска айди отчета, отправленного владельцем питомца сегодня
     * @param chatId идентификатор чата
     * @return айди искомого отчета
     */
    private long findReportIdByChatId(Long chatId) {
        CatOwner catOwner = petOwnerService.findCatOwner(chatId);
        DogOwner dogOwner = petOwnerService.findDogOwner(chatId);
        List<KeepingPet> reportsToday = (List<KeepingPet>) getAllKeepingPet(LocalDate.now());
        long reportId = -1; // идентификатор  отчета, отправленного пользователем сегодня
        if (catOwner != null) {
            for (KeepingPet keepingPet : reportsToday) {
                if (keepingPet.getCatOwner().equals(catOwner)) {
                    reportId = keepingPet.getId();
                    break;
                }
            }
        } else if (dogOwner != null) {
            for (KeepingPet keepingPet : reportsToday) {
                if (keepingPet.getDogOwner().equals(dogOwner)) {
                    reportId = keepingPet.getId();
                    break;
                }
            }
        } else {
            throw new IllegalArgumentException("Владельца с таким chatId не существует:" + chatId);
        }
        return reportId;
    }

    /**
     * Метод создает объект типа PhotoPet
     *
     * @param chatId Идентификатор чата
     * @param fileRequest объект класса GetFile
     * @param file объект класса File
     * @param filePath путь к файлу
     * @return созданный объект класса PhotoPet
     */
    private PhotoPet createPhotoPet(Long chatId, GetFile fileRequest, File file, Path filePath) {

        PhotoPet photoPet = new PhotoPet();
        photoPet.setMediaType(fileRequest.getContentType());
        photoPet.setFileSize(file.fileSize());
        photoPet.setFilePath(filePath.toString());

        CatOwner catOwner = petOwnerService.findCatOwner(chatId);
        DogOwner dogOwner = petOwnerService.findDogOwner(chatId);
        if (catOwner != null) {
            photoPet.setPet(catOwner.getPet());
        }else if (dogOwner != null) {
            photoPet.setPet(dogOwner.getPet());
        }else {
            throw new IllegalArgumentException("Владельца с таким chatId не существует:" + chatId);
        }

        return photoPet;
    }
    /**
     * Метод обновляет объект типа PhotoPet
     *
     * @param reportId айди отчета о питомце
     * @param fileRequest объект класса GetFile
     * @param file объект класса File
     * @param filePath путь к файлу
     * @return созданный объект класса PhotoPet
     */
    private PhotoPet updatePhotoPet(Long reportId, GetFile fileRequest, File file, Path filePath) {

        KeepingPet keepingPet = keepingPetRepository.findKeepingPetById(reportId);

        PhotoPet photoPet = keepingPet.getPhotoPet();

        photoPet.setMediaType(fileRequest.getContentType());
        photoPet.setFileSize(file.fileSize());
        photoPet.setFilePath(filePath.toString());

        return photoPet;

    }

    /**
     * /метод создает новый отчет о питомце
     * @param chatId идентификатор чата
     * @param caption текстовое сообщение к фотографии
     * @param photoPet объект содержащий информацию. о фотографии
     * @return новый отчет
     */
    private KeepingPet createKeepingPet(Long chatId, String caption, PhotoPet photoPet) {
        CatOwner catOwner = petOwnerService.findCatOwner(chatId);
        DogOwner dogOwner = petOwnerService.findDogOwner(chatId);

        KeepingPet keepingPet = new KeepingPet();
        keepingPet.setChatId(chatId);
        if (catOwner != null) {
            keepingPet.setCatOwner(catOwner);
        } else if (dogOwner != null) {
            keepingPet.setDogOwner(dogOwner);
        } else {
            throw new IllegalArgumentException("Владельца с таким chatId не существует:" + chatId);
        }
        keepingPet.setDateTime(LocalDateTime.now());
        keepingPet.setInfoPet(caption);
        keepingPet.setPhotoPet(photoPet);
        return keepingPet;
    }
    /**
     * Метод обновляет отчет о питомце.
     * За один день владелец питомца может отправить в БД только один отчет.
     * Если владелец отправляет 2-ой или более отчет в день, то текущий отчет обновляется.
     * @param reportId айди текущего отчета
     * @param caption новый текстовый отчет
     * @param photoPet новое фотография питомца
     * @return обновленный отчет
     */
    private KeepingPet updateKeepingPet(long reportId, String caption, PhotoPet photoPet) {
        KeepingPet keepingPet = keepingPetRepository.findKeepingPetById(reportId);

        keepingPet.setDateTime(LocalDateTime.now());
        keepingPet.setInfoPet(caption);
        keepingPet.setPhotoPet(photoPet);
        return keepingPet;
    }

    /**
     * Метод загружает фотографию питомца на сервер в папку
     * @param fileData массив байтов, хранящий фотографию
     * @param filePath путь для сохранения фотографии
     * @Throw RuntimeException ошибка при сохранении фото
     *
     */
    private void uploadPhotoToServer(byte[] fileData, Path filePath) {
        try (InputStream is = new ByteArrayInputStream(fileData);
             OutputStream os=Files.newOutputStream(filePath,CREATE_NEW);
             BufferedInputStream bis = new BufferedInputStream(is, 1024);
             BufferedOutputStream bos = new BufferedOutputStream(os, 1024);
        ) {
            bis.transferTo(bos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * первая стадия метода отправки отчета. Этот метод отпрвляет пользователю сообщение с просьбой
     * отправить отчет: текст и фото
     *
     * @param chatId      идентификатор чата
     * @param messageText сообщение для пользователя
     */
    public void sendReport(long chatId, String messageText) {
        sendMessageReply(chatId, messageText);
    }

    /**
     * Метод вызывается при отправке отчета пользователем, который
     * не является владельцем питомца
     * @param chatId идентификатор чата
     * @param messageText сообщение пользователю
     */
    public void sendReportWithoutReply(long chatId, String messageText) {
        sendMessage(chatId, messageText);
    }

    /**
     * Метод получает расширение файла из его полного пути
     * @param fileName имя файла
     * @return расширение файла
     */
    private String getExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    private void sendMessageReply(long chatId, String messageText) {
        SendMessage sendMess = new SendMessage(chatId, messageText);
        sendMess.replyMarkup(new ForceReply());
        telegramBot.execute(sendMess);
    }

    public void sendMessage(long chatId, String messageText) {
        SendMessage sendMess = new SendMessage(chatId, messageText);
        telegramBot.execute(sendMess);
    }

    /**
     * метод для волонтера, для отправки усыновителю предупреждения о том,
     * что отчет заполняется плохо
     * @param id - id владельца питомца
     * @param quality - степень качества заполнения отчета
     */
    public void sendWarningByVolunteer(Long id, boolean quality) {
        KeepingPet findKeepingPet = keepingPetRepository.findById(id).get();
        findKeepingPet.setQuality(quality);
        KeepingPet result = keepingPetRepository.save(findKeepingPet);
        if(result.isQuality() == false){
            sendMessage(result.getChatId(), "Дорогой усыновитель, мы заметили, что ты заполняешь отчет не так подробно, как необходимо. Пожалуйста, подойди ответственнее к этому занятию. В противном случае волонтеры приюта будут обязаны самолично проверять условия содержания животного.");
        } else{
            sendMessage(result.getChatId(), "Отчет заполнен качественно. Спасибо.");
        }
    }

    /**
     * Метод выводит список всех отчетов по определенным датам.
     *
     * @return Collection
     */
    public Collection<KeepingPet> getAllKeepingPet(LocalDate date){
        return keepingPetRepository.findKeepingPetByDateTimeBetween(date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }

    /**
     * Метод возвращает список отчетов по айди владельца
     * @param ownerId идентификатор (id) владельца питомца
     * @throws IllegalArgumentException если по айди не найден владелец питомца
     * @return список отчетов
     */
    public Collection<KeepingPet> getAllKeepingPetByOwnerId(Long ownerId) {
        CatOwner catOwner = petOwnerService.findCatOwnerById(ownerId);
        DogOwner dogOwner = petOwnerService.findDogOwnerById(ownerId);
        if (catOwner != null) {
            return keepingPetRepository.findKeepingPetByCatOwner(catOwner);
        } else if (dogOwner != null) {
            return keepingPetRepository.findKeepingPetByDogOwner(dogOwner);
        } else {
            throw new IllegalArgumentException("Владельца с таким айди не существует");
        }
    }

    /**
     * Метод формирует список последних отчетов по каждому владельцу
     * @return List
     */
    public List<KeepingPet> lastKeepingPetOwner(){
        List<KeepingPet> catOwner = keepingPetRepository.findLastKeepingPetCatOwner();
        List<KeepingPet> dogOwner = keepingPetRepository.findLastKeepingPetDogOwner();
        List<KeepingPet> allOwner = Stream.concat(catOwner.stream(), dogOwner.stream())
                .collect(Collectors.toList());
        return allOwner;
    }
}
