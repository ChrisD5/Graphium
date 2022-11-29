package ai.graphium.checkin.repos;

import ai.graphium.checkin.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, Long> {
}
