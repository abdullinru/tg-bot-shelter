package pro.sky.telegrambot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pro.sky.telegrambot.model.KeepingPet;
import pro.sky.telegrambot.service.KeepingPetService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;

public interface KeepingPetRepository extends JpaRepository<KeepingPet,Long> {

    Collection<KeepingPet> findKeepingPetByDateTime(LocalDateTime dateTime);
}