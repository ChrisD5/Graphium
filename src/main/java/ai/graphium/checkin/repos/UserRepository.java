package ai.graphium.checkin.repos;

import ai.graphium.checkin.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface UserRepository extends JpaRepository<User, String> {

    User findByEmail(String email);

    Boolean existsByEmail(String email);

    Collection<User> findFirst10ByEmployee(Boolean is_employee);
}