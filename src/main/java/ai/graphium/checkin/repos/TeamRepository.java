package ai.graphium.checkin.repos;

import ai.graphium.checkin.entity.Team;
import ai.graphium.checkin.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamRepository extends JpaRepository<Team, String> {

    List<Team> findAll();

    Team findById(Long id);

//    List<Team> findBySupervisorExistsIsFalse();

    Team findBySupervisor(User user);

    boolean existsById(Long id);

    Team findBySupervisorEmail(String email);

    boolean existsByName(String name);

    boolean existsBySupervisorId(long id);
}
