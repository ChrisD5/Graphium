package ai.graphium.checkin.repos;

import ai.graphium.checkin.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {
}
