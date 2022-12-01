package ai.graphium.checkin.repos;

import ai.graphium.checkin.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface UserRepository extends JpaRepository<User, String> {

    List<User> findAllByEmployeeAndSupervisorAndAdmin(boolean isEmployee, boolean isSupervisor, boolean isAdmin);

    List<User> findAllByTeamId(long id);

    User findById(long id);

    User findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsById(long id);

    Collection<User> findByEmployeeAndSupervisorIsFalse(Boolean is_employee);

    Collection<User> findBySupervisorIsTrue();

    Collection<User> findByTeamIdAndSupervisorIsFalse(long teamId);

    Collection<User> findFirst10BySupervisorIsTrue();

}