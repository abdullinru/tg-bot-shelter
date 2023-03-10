package pro.sky.telegrambot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pro.sky.telegrambot.model.DogOwner;

import java.util.Collection;

public interface DogOwnerRepository extends JpaRepository<DogOwner,Long> {
    DogOwner findDogOwnerByChatId(Long chatId);
    DogOwner findDogOwnerById(Long Id);
}
