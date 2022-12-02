package ai.graphium.checkin.entity;

import ai.graphium.checkin.enums.AlertType;
import ai.graphium.checkin.enums.AlertVisibility;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@NoArgsConstructor
@Getter
@Setter
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @ManyToOne
    @JoinColumn(name = "target_id")
    private User target;
    @ManyToOne
    @JoinColumn(name = "supervisor_id")
    private User supervisor;

    @Column
    private String title;

    @Column(name = "target_message", columnDefinition = "text")
    private String targetMessage;

    @Column(name = "supervisor_message", columnDefinition = "text")
    private String supervisorMessage;

    @Enumerated(EnumType.ORDINAL)
    private AlertType type;

    @Column(name = "read_by_target")
    private boolean readByTarget;

    @Column(name = "read_by_supervisor")
    private boolean readBySupervisor;

    @Enumerated(EnumType.ORDINAL)
    private AlertVisibility visibility;

    @Column(name = "created_at")
    private long created;

    public Alert(User target, User supervisor, String title, String targetMessage, String supervisorMessage, AlertType type, AlertVisibility visibility) {
        this(target, supervisor, title, targetMessage, supervisorMessage, System.currentTimeMillis(), type, visibility);
    }

    public Alert(User target, User supervisor, String title, String targetMessage, String supervisorMessage, long created, AlertType type, AlertVisibility visibility) {
        this.target = target;
        this.supervisor = supervisor;
        this.title = title;
        this.targetMessage = targetMessage;
        this.supervisorMessage = supervisorMessage;
        this.created = created;
        this.type = type;
        this.visibility = visibility;
    }
}
