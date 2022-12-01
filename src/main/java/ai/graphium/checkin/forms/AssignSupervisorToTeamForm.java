package ai.graphium.checkin.forms;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AssignSupervisorToTeamForm {

    private long supervisor_id;
    private long team_id;
}
