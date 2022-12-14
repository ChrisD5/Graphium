package ai.graphium.checkin.repos;

import ai.graphium.checkin.entity.Meeting;
import ai.graphium.checkin.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    List<Meeting> findByRequester_EmailAndTimeGreaterThan(String email, long time);

    List<Meeting> findByRequestee(User requestee);
}
