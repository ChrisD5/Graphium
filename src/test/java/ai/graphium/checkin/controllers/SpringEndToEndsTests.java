package ai.graphium.checkin.controllers;

import ai.graphium.checkin.entity.Alert;
import ai.graphium.checkin.entity.Note;
import ai.graphium.checkin.entity.Team;
import ai.graphium.checkin.entity.User;
import ai.graphium.checkin.enums.AlertType;
import ai.graphium.checkin.enums.AlertVisibility;
import ai.graphium.checkin.enums.NoteType;
import ai.graphium.checkin.enums.UserType;
import ai.graphium.checkin.forms.ToggleEmployeeAlertDayForm;
import ai.graphium.checkin.repos.AlertRepository;
import ai.graphium.checkin.repos.NoteRepository;
import ai.graphium.checkin.repos.TeamRepository;
import ai.graphium.checkin.repos.UserRepository;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
public class SpringEndToEndsTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private AlertRepository alertRepository;


    @Before  // setting up users and alerts
    public void userSetup() {
        var pass = "frog";
        var maxjones = userRepository
                .findByEmail("maxjones@graphium.ai");
        maxjones = new User("maxjones@graphium.ai", passwordEncoder.encode(pass), UserType.EMPLOYEE, "Max Jones", "+441234567666");
        userRepository.save(maxjones);
        var andrewsharp = userRepository
                .findByEmail("andrewsharp@graphium.ai");
        if (andrewsharp == null) {
            andrewsharp = new User("andrewsharp@graphium.ai", passwordEncoder.encode(pass), UserType.SUPERVISOR, "Andrew Sharp", "+441234567890");
            userRepository.save(andrewsharp);
        }
        alertRepository.save(new Alert(
                maxjones, andrewsharp,
                "Test",
                "Test",
                "Test",
                System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7),
                AlertType.HIGH, AlertVisibility.ALL
        ));
    }


    @Test
        //testing function of promoting an employee to supervisor by reassigning role
    void testPromotingEmployeeToSupervisor() {
        //given max jones is an employee
        var maxjones = userRepository
                .findByEmail("maxjones@graphium.ai");
        //when max jones is promoted to supervisor
        maxjones.setSupervisor(true);
        assert maxjones.isSupervisor();
        assert userRepository.save(maxjones).isSupervisor();
        //then the supervisor user repository should contain max jones
        assert userRepository.findAll().stream().anyMatch(User::isSupervisor);


    }

    @Test //testing function of demoting supervisor to employee by reassigning role
    public void testDemotingSupervisorToEmployee2() {
        //given andrew sharp is a supervisor
        var andrewsharp = userRepository
                .findByEmail("andrewsharp@graphium.ai");
        //when andrew sharp is demoted to employee
        andrewsharp.setSupervisor(false);
        assert andrewsharp.isEmployee();
        //then the employee user repository should contain andrew sharp
        assert userRepository.findAll().stream().anyMatch(User::isEmployee);
    }

    @Test //testing function of adding a new team and assigning employee to said team
    public void testAssigningEmployeeToTeam() {
        var maxjones = userRepository
                .findByEmail("maxjones@graphium.ai");
        //given max jones is an employee
        maxjones.setSupervisor(true);
        var team1 = new Team(maxjones, "Team one");
        //when max jones is assigned to a new team
        teamRepository.save(team1);
        maxjones.setTeam(team1);
        //then the team repository should contain team one
        assert maxjones.getTeam().equals(team1);
    }

    @Test //testing function for adding a new team
    public void testAddingAnotherTeam() {
        var maxjones = userRepository
                .findByEmail("maxjones@graphium.ai");
        maxjones.setSupervisor(true);
        var andrewsharp = userRepository
                .findByEmail("andrewsharp@graphium.ai");
        //Given that there is one team
        var team1 = new Team(andrewsharp, "Team one");
        andrewsharp.setTeam(team1);
        teamRepository.save(team1);
        Integer originalSize = teamRepository.findAll().size();
        //When a new team is added
        var team2 = new Team(maxjones, "Team two");
        maxjones.setTeam(team2);
        teamRepository.save(team2);
        //Then the team repository should contain two teams
        assert teamRepository.findAll().size() == originalSize + 1;
    }

    @Test //testing function for removing a team
    public void testRemovingATeam() {
        var maxjones = userRepository
                .findByEmail("maxjones@graphium.ai");
        maxjones.setSupervisor(true);
        var andrewsharp = userRepository
                .findByEmail("andrewsharp@graphium.ai");
        andrewsharp.setSupervisor(true);
        //Given that there is two teams team
        var team1 = new Team(andrewsharp, "Team one");
        teamRepository.save(team1);
        var team2 = new Team(maxjones, "Team two");
        teamRepository.save(team2);
        Integer originalSize = teamRepository.findAll().size();
        //When a team is removed
        teamRepository.delete(team1);
        System.out.println("team size is" + teamRepository.findAll().size());
        //Then the team repository should contain one team
        assert teamRepository.findAll().size() == originalSize - 1;

    }

    @Test //testing function of disabling alerts for a specific employee
    public void testEmployeeDisablingAlert() {
        //Given that max jones is an employee
        var maxjones = userRepository
                .findByEmail("maxjones@graphium.ai");
        //When max jones disables alerts
        maxjones.setSettingsAlertDisabled(true);
        //Then max's alert repository should not contain any alerts
        assert maxjones.isSettingsAlertDisabled();

    }

    @Test //testing function of enabling alerts for a specific employee
    public void testEmployeeEnablingAlert() {
        //Given that max jones is an employee
        var maxjones = userRepository
                .findByEmail("maxjones@graphium.ai");
        //When max jones enables alerts
        maxjones.setSettingsAlertDisabled(false);
        //Then max's alert repository should contain alerts
        assert !maxjones.isSettingsAlertDisabled();

    }

    @Test //testing disabling alerts on specific days
    public void testEmployeeDisablingAlertOnSpecificDays() {
        //Given that max jones is an employee
        var maxjones = userRepository
                .findByEmail("maxjones@graphium.ai");
        //When max jones disables alerts on specific days
        var toggleEmployeeAlertDayForm = new ToggleEmployeeAlertDayForm();
        String day = toggleEmployeeAlertDayForm.getDay();
        if (maxjones.getSettingsAlertDayDisabled().contains(day)) {
            maxjones.getSettingsAlertDayDisabled().remove(day);
        } else {
            maxjones.getSettingsAlertDayDisabled().add(day);
        }
        //Then max's alert repository should not contain alerts on those days
        assert maxjones.getSettingsAlertDayDisabled().contains(day);
    }

    @Test //testing notes are saved
    public void shouldSaveNote() throws Exception {
        //Given the notes repository is empty
        Integer oldNotes = noteRepository.findAll().size();
        //When a note is added
        Note note = new Note("Hello", NoteType.CHECKIN);
        noteRepository.save(note);
        Integer newNotes = noteRepository.findAll().size();
        //Then the notes repository should contain one note
        assert newNotes > oldNotes;

    }
}