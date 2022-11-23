package ai.graphium.checkin.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/a")
public class AdminController {

    @GetMapping("")
    public String adminHomeController() {
        return "admin/index";
    }
}
