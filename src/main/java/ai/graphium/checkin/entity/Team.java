package ai.graphium.checkin.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "teams")
public class Team {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false)
    private long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_supervisor_id", nullable = false)
    private User supervisor;

    @Column(nullable = false)
    private String name;

    public Team(User supervisor, String name) {
        this.supervisor = supervisor;
        this.name = name;
    }

}
