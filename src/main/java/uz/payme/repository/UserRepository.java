package uz.payme.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uz.payme.entity.User;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    @Query("select u from User u where u.phoneNumber = ?1")
    @EntityGraph(attributePaths = "role")
    Optional<User> findByPhoneNumber(String phoneNumber);

    @Query("select u from User u where u.phoneNumber = ?1 or u.pinfl = ?2")
    Optional<User> findByPhoneNumberOrPinfl(String phoneNumber, String pinfl);

    Optional<User> findByPinfl(String pinfl);

    @Query(nativeQuery = true, value = "select * from users u inner join application a on u.id = a.user_id where a.id=?1 ")
    Optional<User> findUserByAppId(String appId);
}