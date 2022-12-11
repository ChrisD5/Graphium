package ai.graphium.checkin.forms;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SetEmployeeReminderTime {
    private String stringTime;
    public SetEmployeeReminderTime(String time) {
        this.stringTime = time;
    }
}
