package ai.graphium.checkin.forms;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateSupervisorForm {

    private String name;
    private String email;
    private String phone;
}
