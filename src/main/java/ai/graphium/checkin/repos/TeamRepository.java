package ai.graphium.checkin.repos;

import ai.graphium.checkin.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamRepository extends JpaRepository<Team, String> {

    List<Team> findAll();

    Team findById(Long id);

    boolean existsById(Long id);

    Team findBySupervisorEmail(String email);
}
