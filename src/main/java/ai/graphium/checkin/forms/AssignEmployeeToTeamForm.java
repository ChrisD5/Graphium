package ai.graphium.checkin.forms;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AssignEmployeeToTeamForm {

    private long team_id;
    private long employee_id;
}
