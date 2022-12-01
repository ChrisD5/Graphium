package ai.graphium.checkin.forms;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class EditEmployeeForm {
    private String name;
    private String phone;
    private MultipartFile image;

    public EditEmployeeForm(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }
}
