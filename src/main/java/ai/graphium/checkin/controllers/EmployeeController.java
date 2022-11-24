package ai.graphium.checkin.controllers;

import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/e")
@Secured("ROLE_EMPLOYEE")
public class EmployeeController {

    @GetMapping("")
    public String employeeHomeController() {
        return "employee/index";
    }

    @GetMapping("/settings")
    public String employeeSettingsController() {
        return "employee/settings";
    }
}
