package ai.graphium.checkin.forms;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SetSupervisorAlertThresholdForm {
    private int threshold;

    public SetSupervisorAlertThresholdForm(int threshold) {
        this.threshold = threshold;
    }
}
