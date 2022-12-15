package ai.graphium.checkin.config;

import ai.graphium.checkin.entity.*;
import ai.graphium.checkin.enums.AlertType;
import ai.graphium.checkin.enums.AlertVisibility;
import ai.graphium.checkin.enums.NoteType;
import ai.graphium.checkin.enums.UserType;
import ai.graphium.checkin.properties.AuthProperties;
import ai.graphium.checkin.repos.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Configuration
public class BootConfig {

    private static final String DEFAULT_USERNAME = "admin@graphium.ai";
    private static final String DEFAULT_PASSWORD = "admin";

    @Autowired
    @Transactional
    public void configure(PasswordEncoder passwordEncoder,
                          UserRepository userRepository,
                          TeamRepository teamRepository,
                          AlertRepository alertRepository,
                          NoteRepository noteRepository,
                          CheckInRepository checkInRepository,
                          AuthProperties authProperties) {

        // adding default admin user
        var admin = userRepository
                .findByEmail(DEFAULT_USERNAME);
        if (admin == null) {
            admin = new User(DEFAULT_USERNAME, passwordEncoder.encode(DEFAULT_PASSWORD), UserType.ADMIN, "Admin", "+441234567890");
            admin.setTotpSecret("FEXOVM5CZRJXWXCLVLPGIVRV2BVMLJGU");
            userRepository.save(admin);
        } else {
            return;
        }
        var pass = "frog";

        if (authProperties.isGenerateDefaultUsers()) {
            var andrewsharp = userRepository
                    .findByEmail("andrewsharp@graphium.ai");
            if (andrewsharp == null) {
                andrewsharp = new User("andrewsharp@graphium.ai", passwordEncoder.encode(pass), UserType.SUPERVISOR, "Andrew Sharp", "+441234567890");
                andrewsharp.setEmployee(true);
                userRepository.save(andrewsharp);
            }
            var maxjones = userRepository
                    .findByEmail("maxjones@graphium.ai");
            if (maxjones == null) {
                maxjones = new User("maxjones@graphium.ai", passwordEncoder.encode(pass), UserType.EMPLOYEE, "Max Jones", "+441234567666");
                userRepository.save(maxjones);
            }
            var pollysmith = userRepository
                    .findByEmail("pollysmith@graphium.ai");
            if (pollysmith == null) {
                pollysmith = new User("pollysmith@graphium.ai", passwordEncoder.encode(pass), UserType.EMPLOYEE, "Polly Smith", "+441234567666");
                userRepository.save(pollysmith);
            }
            var jeromeclacy = userRepository
                    .findByEmail("jeromeclacy@graphium.ai");
            if (jeromeclacy == null) {
                jeromeclacy = new User("jeromeclacy@graphium.ai", passwordEncoder.encode(pass), UserType.EMPLOYEE, "Jerome Clacy", "+441234567666");
                userRepository.save(jeromeclacy);
            }
            var michaelsteer = userRepository
                    .findByEmail("michaelsteer@graphium.ai");
            if (michaelsteer == null) {
                michaelsteer = new User("michaelsteer@graphium.ai", passwordEncoder.encode(pass), UserType.EMPLOYEE, "Michael Steer", "+4412345676");
                userRepository.save(michaelsteer);
            }
            var alicehitchin = userRepository
                    .findByEmail("alicehitchin@graphium.ai");
            if (alicehitchin == null) {
                alicehitchin = new User("alicehitchin@graphium.ai", passwordEncoder.encode(pass), UserType.EMPLOYEE, "Alice Hitchin", "+441234567666");
                userRepository.save(alicehitchin);
            }
            var emailEmployee = userRepository
                    .findByEmail("kavin@kavin.rocks");
            if (emailEmployee == null) {
                emailEmployee = new User("kavin@kavin.rocks", passwordEncoder.encode("kavin"), UserType.EMPLOYEE, "Kavin", "+441234567890");
                userRepository.save(emailEmployee);
            }
            var emailSupervisor = userRepository
                    .findByEmail("supervisor@kavin.rocks");
            if (emailSupervisor == null) {
                emailSupervisor = new User("supervisor@kavin.rocks", passwordEncoder.encode("supervisor"), UserType.SUPERVISOR, "Supervisor", "+441234567890");
                userRepository.save(emailSupervisor);
            }
            var carlJones = userRepository
                    .findByEmail("jonesc162@cardiff.ac.uk");
            if (carlJones == null) {
                carlJones = new User("jonesc162@cardiff.ac.uk", passwordEncoder.encode("frog"), UserType.EMPLOYEE, "Carl Jones", "+441234567890");
                carlJones.setSupervisor(true);
                userRepository.save(carlJones);
            }
            var jamesSmith = userRepository
                    .findByEmail("jamessmith@graphium.ai");
            if (jamesSmith == null) {
                jamesSmith = new User("jamessmith@graphium.ai", passwordEncoder.encode("frog"), UserType.EMPLOYEE, "James Smith", "+441234567890");
                userRepository.save(jamesSmith);
            }
            var teams = teamRepository
                    .findAll();
            if (teams.size() < 2) {
                teamRepository.deleteAll();
                var team1 = new Team(andrewsharp, "Team Alpha");

                emailEmployee.setTeam(team1);
                maxjones.setTeam(team1);
                michaelsteer.setTeam(team1);
                pollysmith.setTeam(team1);
                jeromeclacy.setTeam(team1);
                alicehitchin.setTeam(team1);
                teamRepository.save(team1);

                carlJones.setTeam(team1);

                var team2 = new Team(carlJones, "Team Beta");
                teamRepository.save(team2);
                jamesSmith.setTeam(team2);
            }
            userRepository.save(emailEmployee);
            userRepository.save(maxjones);
            userRepository.save(michaelsteer);
            userRepository.save(pollysmith);
            userRepository.save(jeromeclacy);
            userRepository.save(alicehitchin);
            userRepository.save(carlJones);
            userRepository.save(jamesSmith);

            {
                var note = new Note("Felt happy with work today.", NoteType.CHECKIN);
                noteRepository.save(note);
                var checkIn = new CheckIn(7, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7), note);
                checkInRepository.save(checkIn);
                maxjones.getCheckIns().add(checkIn);
            }
            {
                var note = new Note("I am feeling tired today.", NoteType.CHECKIN);
                noteRepository.save(note);
                var checkIn = new CheckIn(9, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7), note);
                checkInRepository.save(checkIn);
                pollysmith.getCheckIns().add(checkIn);
            }
            {

                var checkIn = new CheckIn(4, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7), null);
                checkInRepository.save(checkIn);
                jeromeclacy.getCheckIns().add(checkIn);
            }
            {
                var note = new Note("I didn't feel happy today", NoteType.CHECKIN);
                noteRepository.save(note);
                var checkIn = new CheckIn(2, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7), note);
                checkInRepository.save(checkIn);
                alicehitchin.getCheckIns().add(checkIn);
            }

            {
                var note = new Note("Im Upset.", NoteType.CHECKIN);
                noteRepository.save(note);
                var checkIn = new CheckIn(3, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(6), note);
                checkInRepository.save(checkIn);
                maxjones.getCheckIns().add(checkIn);
            }
            {

                var checkIn = new CheckIn(8, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(6), null);
                checkInRepository.save(checkIn);
                michaelsteer.getCheckIns().add(checkIn);
            }
            {
                var note = new Note("I feeling angry.", NoteType.CHECKIN);
                noteRepository.save(note);
                var checkIn = new CheckIn(4, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(6), note);
                checkInRepository.save(checkIn);
                pollysmith.getCheckIns().add(checkIn);
            }
            {
                var note = new Note("Im mildly upset.", NoteType.CHECKIN);
                noteRepository.save(note);
                var checkIn = new CheckIn(1, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(6), note);
                checkInRepository.save(checkIn);
                jeromeclacy.getCheckIns().add(checkIn);
            }
            {
                var note = new Note("I didn't eat breakfast today", NoteType.CHECKIN);
                noteRepository.save(note);
                var checkIn = new CheckIn(8, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(6), note);
                checkInRepository.save(checkIn);
                alicehitchin.getCheckIns().add(checkIn);
            }

            {
                var note = new Note("Julia has really upset me today.", NoteType.CHECKIN);
                noteRepository.save(note);
                var checkIn = new CheckIn(7, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(5), note);
                checkInRepository.save(checkIn);
                maxjones.getCheckIns().add(checkIn);
            }
            {
                var note = new Note("I'm not angry, I'm just disjointed.", NoteType.CHECKIN);
                noteRepository.save(note);
                var checkIn = new CheckIn(7, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(5), note);
                checkInRepository.save(checkIn);
                michaelsteer.getCheckIns().add(checkIn);
            }
            {
                var note = new Note("I am feeling rather ecstatic today", NoteType.CHECKIN);
                noteRepository.save(note);
                var checkIn = new CheckIn(5, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(5), note);
                checkInRepository.save(checkIn);
                pollysmith.getCheckIns().add(checkIn);
            }
            {
                var checkIn = new CheckIn(3, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(5), null);
                checkInRepository.save(checkIn);
                alicehitchin.getCheckIns().add(checkIn);
            }

            {
                var checkIn = new CheckIn(5, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(4), null);
                checkInRepository.save(checkIn);
                maxjones.getCheckIns().add(checkIn);
            }
            {
                var note = new Note("Im Upset.", NoteType.CHECKIN);
                noteRepository.save(note);
                var checkIn = new CheckIn(10, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(4), note);
                checkInRepository.save(checkIn);
                michaelsteer.getCheckIns().add(checkIn);
            }
            {
                var note = new Note("I am feeling tired today.", NoteType.CHECKIN);
                noteRepository.save(note);
                var checkIn = new CheckIn(3, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(4), note);
                checkInRepository.save(checkIn);
                pollysmith.getCheckIns().add(checkIn);
            }
            {
                var note = new Note("Im Upset.", NoteType.CHECKIN);
                noteRepository.save(note);
                var checkIn = new CheckIn(10, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(4), note);
                checkInRepository.save(checkIn);
                jeromeclacy.getCheckIns().add(checkIn);
            }
            {
                var note = new Note("I didn't feel happy today", NoteType.CHECKIN);
                noteRepository.save(note);
                var checkIn = new CheckIn(2, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(4), note);
                checkInRepository.save(checkIn);
                alicehitchin.getCheckIns().add(checkIn);
            }

            {
                var note = new Note("I've just won the lottery.", NoteType.CHECKIN);
                noteRepository.save(note);
                var checkIn = new CheckIn(10, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(3), note);
                checkInRepository.save(checkIn);
                maxjones.getCheckIns().add(checkIn);
            }
            {
                var note = new Note("It was raining so im less happy.", NoteType.CHECKIN);
                noteRepository.save(note);
                var checkIn = new CheckIn(7, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(3), note);
                checkInRepository.save(checkIn);
                michaelsteer.getCheckIns().add(checkIn);
            }
            {
                var checkIn = new CheckIn(10, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(3), null);
                checkInRepository.save(checkIn);
                pollysmith.getCheckIns().add(checkIn);
            }
            {
                var note = new Note("Feel average.", NoteType.CHECKIN);
                noteRepository.save(note);
                var checkIn = new CheckIn(5, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(3), note);
                checkInRepository.save(checkIn);
                jeromeclacy.getCheckIns().add(checkIn);
            }
            {
                var note = new Note("Loving life!", NoteType.CHECKIN);
                noteRepository.save(note);
                var checkIn = new CheckIn(8, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(3), note);
                checkInRepository.save(checkIn);
                alicehitchin.getCheckIns().add(checkIn);
            }

            {
                var note = new Note("Feel a bit meh today.", NoteType.CHECKIN);
                noteRepository.save(note);
                var checkIn = new CheckIn(6, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2), note);
                checkInRepository.save(checkIn);
                maxjones.getCheckIns().add(checkIn);
            }
            {
                var note = new Note("I feel a bit la-de-dah today.", NoteType.CHECKIN);
                noteRepository.save(note);
                var checkIn = new CheckIn(6, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2), note);
                checkInRepository.save(checkIn);
                michaelsteer.getCheckIns().add(checkIn);
            }
            {
                var checkIn = new CheckIn(9, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2), null);
                checkInRepository.save(checkIn);
                jeromeclacy.getCheckIns().add(checkIn);
            }
            {
                var note = new Note("I didn't any emotion today", NoteType.CHECKIN);
                noteRepository.save(note);
                var checkIn = new CheckIn(2, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2), note);
                checkInRepository.save(checkIn);
                alicehitchin.getCheckIns().add(checkIn);
            }

            {
                var note = new Note("My feline-friend passed away.", NoteType.CHECKIN);
                noteRepository.save(note);
                var checkIn = new CheckIn(1, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1), note);
                checkInRepository.save(checkIn);
                maxjones.getCheckIns().add(checkIn);
            }
            {
                var checkIn = new CheckIn(6, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1), null);
                checkInRepository.save(checkIn);
                michaelsteer.getCheckIns().add(checkIn);
            }
            {
                var note = new Note("I am feeling fatigued from work.", NoteType.CHECKIN);
                noteRepository.save(note);
                var checkIn = new CheckIn(3, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1), note);
                checkInRepository.save(checkIn);
                pollysmith.getCheckIns().add(checkIn);
            }
            {
                var note = new Note("Im just feeling so average.", NoteType.CHECKIN);
                noteRepository.save(note);
                var checkIn = new CheckIn(5, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1), note);
                checkInRepository.save(checkIn);
                jeromeclacy.getCheckIns().add(checkIn);
            }
            {
                var note = new Note("I just found out how to centre a div!!!", NoteType.CHECKIN);
                noteRepository.save(note);
                var checkIn = new CheckIn(8, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1), note);
                checkInRepository.save(checkIn);
                alicehitchin.getCheckIns().add(checkIn);
            }

            alertRepository.save(new Alert(
                    jeromeclacy, andrewsharp,
                    "Missing check in",
                    "You have not checked in today. Please do not forget to check-in!",
                    "Your employee " + jeromeclacy.getName() + " has not checked in today.",
                    System.currentTimeMillis() - TimeUnit.DAYS.toMillis(5),
                    AlertType.HIGH, AlertVisibility.ALL
            ));
            alertRepository.save(new Alert(
                    pollysmith, andrewsharp,
                    "Missing check in",
                    "You have not checked in today. Please do not forget to check-in!",
                    "Your employee " + pollysmith.getName() + " has not checked in today.",
                    System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2),
                    AlertType.HIGH, AlertVisibility.ALL
            ));
            alertRepository.save(new Alert(
                    michaelsteer, andrewsharp,
                    "Missing check in",
                    "You have not checked in today. Please do not forget to check-in!",
                    "Your employee " + michaelsteer.getName() + " has not checked in today.",
                    System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7),
                    AlertType.HIGH, AlertVisibility.ALL
            ));


            userRepository.save(maxjones);
            userRepository.save(michaelsteer);
            userRepository.save(pollysmith);
            userRepository.save(jeromeclacy);
            userRepository.save(alicehitchin);
        }
    }
}
