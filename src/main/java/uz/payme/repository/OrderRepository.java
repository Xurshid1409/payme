package uz.payme.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.payme.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

}
