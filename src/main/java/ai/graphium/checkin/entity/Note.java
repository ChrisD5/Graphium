package ai.graphium.checkin.entity;

import ai.graphium.checkin.enums.NoteType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@NoArgsConstructor
@Entity
@Getter
public class Note {

    @Id
    @GeneratedValue
    private long id;

    @Column(columnDefinition = "text", nullable = false)
    private String text;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private NoteType type;

    public Note(String text, NoteType type) {
        this.text = text;
        this.type = type;
    }

}
