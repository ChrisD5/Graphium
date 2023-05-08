package ai.graphium.checkin.repos;

import ai.graphium.checkin.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    List<User> findAllByEmployeeAndSupervisorAndAdmin(boolean isEmployee, boolean isSupervisor, boolean isAdmin);

    List<User> findAllByTeamId(long id);

    User findById(long id);

    User findByEmail(String email);

    Optional<Long> findIdByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsById(long id);

    List<User> findByEmployeeAndSupervisorIsFalse(Boolean is_employee);

    Collection<User> findBySupervisorIsTrue();

    List<User> findByTeamIdAndSupervisorIsFalseAndEmployeeIsTrue(long teamId);

    int countAllByEmployeeIsTrueAndSupervisorIsFalse();

    int countAllBySupervisorIsTrue();

    List<User> findAllBySupervisor(boolean supervisor);

}