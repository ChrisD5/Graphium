package ai.graphium.checkin.entity.joins;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SupervisorJoinTeam {

    private long id;
    private String name;
    private String teamname;

    public SupervisorJoinTeam(long id, String name) {
        this.id = id;
        this.name = name;
    }
}
