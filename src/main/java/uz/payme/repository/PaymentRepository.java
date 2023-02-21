package uz.payme.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.payme.entity.Payment;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findFirstByOrderTransactionIdOrderByPayDateDesc(Long orderTransactionId);


}
