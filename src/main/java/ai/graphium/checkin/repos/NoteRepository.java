package ai.graphium.checkin.repos;

import ai.graphium.checkin.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteRepository extends JpaRepository<Note, Long> {

}
