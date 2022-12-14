package ai.graphium.checkin.entity.joins;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EmployeeJoinCheckIn {
    private long id;
    private boolean disabled;
    private String name;
    private String teamname;
    private Double avgrating = Double.valueOf(0);

    public EmployeeJoinCheckIn(long id, String name, boolean disabled) {
        this.id = id;
        this.name = name;
        this.disabled = disabled;
    }
}
