package ai.graphium.checkin.entity;

import ai.graphium.checkin.enums.UserType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "users")
public class User {

    public User(String email, String password, UserType userType) {
        this.email = email;
        this.password = password;
        switch (userType) {
            case EMPLOYEE -> this.employee = true;
            case SUPERVISOR -> this.supervisor = true;
            case ADMIN -> this.admin = true;
        }
    }

    @Id
    @Column(unique = true)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(length = 50)
    private String email;

    @Column(columnDefinition = "text")
    private String password;

    private boolean disabled;

    private boolean employee;

    private boolean supervisor;

    private boolean admin;

}