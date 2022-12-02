package ai.graphium.checkin.services;

import ai.graphium.checkin.entity.CheckIn;
import ai.graphium.checkin.entity.User;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.criteria.JoinType;
import java.util.concurrent.TimeUnit;

@AllArgsConstructor
@Service
public class EmployeeService {

    private EntityManager entityManager;

    public long getLastCheckin(String email) {
        // Get the current user
        var cb = entityManager.getCriteriaBuilder();
        var cq = cb.createQuery(CheckIn.class);
        var root = cq.from(CheckIn.class);

        cq.select(root);
        {
            var sq = cq.subquery(Long.class);
            var sqRoot = sq.from(User.class);
            var checkInJoin = sqRoot.join("checkIns", JoinType.LEFT);
            sq.select(cb.max(checkInJoin.get("time")));
            sq.where(cb.and(
                    cb.equal(sqRoot.get("email"), email)
            ));
            cq.where(cb.and(
                    // This may fail when two users check in at the exact same millisecond
                    cb.equal(root.get("time"), sq)
            ));
        }

        long lastCheckin;

        try {
            lastCheckin = entityManager.createQuery(cq).setMaxResults(1).getSingleResult().getTime();
        } catch (NoResultException e) {
            lastCheckin = -1;
        }
        return lastCheckin;
    }

    public boolean hasCheckedInToday(String email) {
        long lastCheckin = this.getLastCheckin(email);

        lastCheckin = lastCheckin - (lastCheckin % TimeUnit.HOURS.toMillis(24));

        return System.currentTimeMillis() - lastCheckin < TimeUnit.HOURS.toMillis(24);
    }
}
