package ai.graphium.checkin.entity.joins;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TeamJoinCheckIn {

    private long id;
    private String supervisor;
    private String name;
    private Double avgrating = Double.valueOf(0);

    public TeamJoinCheckIn(long id, String supervisor, String name) {
        this.id = id;
        this.supervisor = supervisor;
        this.name = name;
    }
}
