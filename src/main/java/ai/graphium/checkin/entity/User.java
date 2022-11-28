package ai.graphium.checkin.entity;

import ai.graphium.checkin.enums.UserType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.sql.Date;
import java.util.Calendar;
import java.util.Set;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "users")
public class User {

    public User(String email, String password, UserType userType, String name, String phone) {
        this.email = email;
        this.password = password;
        switch (userType) {
            case EMPLOYEE -> this.employee = true;
            case SUPERVISOR -> this.supervisor = true;
            case ADMIN -> this.admin = true;
        }
        this.name = name;
        this.phone = phone;
        this.created = new Date(Calendar.getInstance().getTime().getTime());
    }

    @Id
    @Column(unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(length = 50, nullable = false)
    private String email;

    @Column(columnDefinition = "text", nullable = false)
    private String password;

    @Column(nullable = false)
    private boolean disabled;

    @Column(nullable = false)
    private boolean employee;

    @Column(nullable = false)
    private boolean supervisor;

    @Column(nullable = false)
    private boolean admin;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private Date created;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @ElementCollection
    @CollectionTable(
            name = "users_checkins"
    )
    @Cascade(org.hibernate.annotations.CascadeType.ALL)
    private Set<CheckIn> checkIns;

}