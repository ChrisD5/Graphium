package ai.graphium.checkin.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "users")
public class User {

    public User(String email, String password) {
        this.email = email;
        this.password = password;
    }

    @Id
    @Column(length = 50)
    private String email;

    @Column(columnDefinition = "text")
    private String password;


}