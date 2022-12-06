package ai.graphium.checkin.entity;

import ai.graphium.checkin.enums.UserType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.sql.Date;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "users")
public class User {

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
    @Column(length = 67000)
    private byte[] image;
    @Column(nullable = false)
    private Date created;
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "team_id")
    private Team team;
    @SuppressWarnings("JpaAttributeTypeInspection")
    @ElementCollection
    @CollectionTable(
            name = "users_checkins"
    )
    @Cascade(org.hibernate.annotations.CascadeType.ALL)
    private Set<CheckIn> checkIns;

    @OneToMany(mappedBy = "target", cascade = CascadeType.ALL)
    private Set<Alert> alerts;

    @Column(nullable = false, columnDefinition = "int default 1")
    private int SettingsAlertThreshold = 1;


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
        this.checkIns = new HashSet<>();
    }
}