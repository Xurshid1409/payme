package uz.payme.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.payme.entity.Client;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Integer> {

    Optional<Client> findByPhoneNumber(String username);
    boolean existsByPhoneNumber(String username);
}
