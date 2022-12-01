package ai.graphium.checkin.entity.joins;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SupervisorJoinTeam {

    private long id;
    private boolean disabled;
    private String name;
    private String teamname;

    public SupervisorJoinTeam(long id, String name, boolean disabled) {
        this.id = id;
        this.name = name;
        this.disabled = disabled;
    }
}
