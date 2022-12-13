package ai.graphium.checkin.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@NoArgsConstructor
@Entity
@Table(name = "meeting")
@Getter
@Setter
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(name = "time", nullable = false)
    private long time;

    @ManyToOne
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne
    @JoinColumn(name = "requestee_id", nullable = false)
    private User requestee;

    @Column(name = "link", nullable = false)
    private String link;

    @Column(name = "confirmed", nullable = false)
    private boolean confirmed;

    @Column(name = "reminded", nullable = false)
    private boolean reminded;

    public Meeting(long time, User requester, User requestee, String link) {
        this.time = time;
        this.requester = requester;
        this.requestee = requestee;
        this.link = link;
        this.confirmed = false;
        this.reminded = false;
    }
}