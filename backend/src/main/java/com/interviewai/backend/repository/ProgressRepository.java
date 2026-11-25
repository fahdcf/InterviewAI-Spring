package com.interviewai.backend.repository;

import com.interviewai.backend.dto.response.LeaderboardEntry;
import com.interviewai.backend.model.entity.ProgressEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ProgressRepository extends JpaRepository<ProgressEvent, Integer> {

    List<XpEvent> findByUserIdAndLastUpdatedGreaterThanEqualOrderByLastUpdatedAscIdAsc(int userId, LocalDateTime start);

    int countByUserIdAndXpGreaterThan(int userId, int xp);

    @Query("select coalesce(sum(p.xp), 0) from ProgressEvent p where p.userId = :userId")
    long findTotalXp(int userId);

    @Query("""
            select coalesce((select sum(p.xp) from ProgressEvent p), 0) * 1.0
                / greatest((select count(u) from UserAccount u), 1)
            """)
    double averageXp();

    @Query("""
            select p.lastUpdated from ProgressEvent p where p.userId = :userId and p.xp > 0
            order by p.lastUpdated desc
            """)
    List<LocalDateTime> findActivityTimes(int userId);

    @Query("""
            select new com.interviewai.backend.dto.response.LeaderboardEntry(u.username, coalesce(sum(p.xp), 0))
            from UserAccount u left join ProgressEvent p on p.userId = u.id
            where u.active = true group by u.id, u.username
            order by coalesce(sum(p.xp), 0) desc, u.username
            """)
    List<LeaderboardEntry> leaderboard(Pageable page);

    @Query("""
            select cast(p.lastUpdated as LocalDate) as day, count(distinct p.userId) as total
            from ProgressEvent p where p.lastUpdated >= :start and p.xp > 0
            group by cast(p.lastUpdated as LocalDate) order by day
            """)
    List<DailyCount> countDailyUsage(LocalDateTime start);

    @Modifying
    @Query("update ProgressEvent p set p.xp = 0 where p.userId = :userId")
    void resetXp(int userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ProgressEvent p where p.userId = :userId")
    void deleteForUser(int userId);

    record XpEvent(LocalDateTime lastUpdated, Integer xp) {
        public int points() {
            return xp == null ? 0 : xp;
        }
    }
}
