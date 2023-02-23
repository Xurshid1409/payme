package uz.payme.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uz.payme.entity.OrderTransaction;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public interface OrderTransactionRepository extends JpaRepository<OrderTransaction, Long> {

    Optional<OrderTransaction> findByTransactionId(String transactionId);
    @Query("select o from OrderTransaction o where o.orderId = ?1")
    Optional<OrderTransaction> findByOrderId(Long orderId);

    List<OrderTransaction> findAllByStateAndTransactionCreationTimeBetween(Integer state, Timestamp fromTransactionCreationTime, Timestamp toTransactionCreationTime);
}
