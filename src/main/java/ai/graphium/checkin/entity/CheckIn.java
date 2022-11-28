package ai.graphium.checkin.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@NoArgsConstructor
@Getter
@Entity
public class CheckIn {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false)
    private long id;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false)
    private long time;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id")
    private Note note;

    public CheckIn(int rating, long time, Note note) {
        this.rating = rating;
        this.time = time;
        this.note = note;
    }
}
