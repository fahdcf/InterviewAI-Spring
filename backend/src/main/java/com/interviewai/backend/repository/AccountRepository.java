package com.interviewai.backend.repository;

import com.interviewai.backend.model.Account;
import com.interviewai.backend.model.entity.UserAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<UserAccount, Integer> {

    Optional<UserAccount> findByUsername(String username);
    Optional<UserAccount> findByEmail(String email);
    boolean existsByIdAndActiveTrueAndSessionVersion(int id, int version);
    int countByActiveTrue();
    int countByCreatedAtGreaterThanEqual(LocalDateTime start);

    default Optional<Account> findByLogin(String identifier) {
        return findByUsername(identifier).or(() -> findByEmail(identifier)).map(AccountRepository::account);
    }

    default Optional<Account> findAccount(int id) {
        return findById(id).map(AccountRepository::account);
    }

    private static Account account(UserAccount user) {
        return new Account(user.getId(), user.getUsername(), user.getEmail(), user.getPasswordHash(),
                user.isActive(), user.getSessionVersion());
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserAccount u where u.id = :id")
    Optional<UserAccount> lockById(int id);

    @Query("select r.name from UserAccount u join u.roles r where u.id = :userId order by r.name")
    List<String> findRoles(int userId);

    @Query("""
            select distinct p.name from UserAccount u join u.roles r join r.permissions p
            where u.id = :userId order by p.name
            """)
    List<String> findPermissions(int userId);

    @Transactional
    @Modifying
    @Query("update UserAccount u set u.passwordHash = :newHash where u.id = :userId and u.passwordHash = :oldHash")
    void upgradePasswordHash(int userId, String oldHash, String newHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update UserAccount u set u.username = :username where u.id = :userId")
    void updateUsername(int userId, String username);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update UserAccount u set u.passwordHash = :password, u.sessionVersion = u.sessionVersion + 1
            where u.id = :userId
            """)
    void changePassword(int userId, String password);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update UserAccount u set u.lastCourseId = :courseId, u.lastChapterId = :chapterId where u.id = :userId")
    void saveLocation(int userId, int courseId, Integer chapterId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update UserAccount u set u.dailyQuestLastReset = :now, u.lastDailyQuestClaim = :today
            where u.id = :userId
            """)
    void recordDailyReset(int userId, LocalDate today, LocalDateTime now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update UserAccount u set u.lastMonthlyQuestClaim = :today where u.id = :userId")
    void recordMonthlyClaim(int userId, LocalDate today);

    @Query("select u.dailyQuestLastReset from UserAccount u where u.id = :userId")
    LocalDateTime dailyReset(int userId);

    @Query("select u.lastMonthlyQuestClaim from UserAccount u where u.id = :userId")
    LocalDate monthlyClaimDate(int userId);

    String ADMIN_USER = """
            select u.id as id, u.username as username, u.email as email, u.active as active, u.createdAt as createdAt,
                   (select coalesce(sum(p.xp), 0) from ProgressEvent p where p.userId = u.id) as xp,
                   (select count(c) from Course c where c.userId = u.id) as courseCount
            from UserAccount u
            """;
    String ADMIN_FILTER = """
            where (lower(u.username) like :search escape '\\'
                or lower(coalesce(u.email, '')) like :search escape '\\')
                and (:status = '' or u.active = case when :status = 'ACTIVE' then true else false end)
                and (:role = '' or exists (select 1 from u.roles r where r.name = :role))
            """;

    @Query(value = ADMIN_USER + ADMIN_FILTER + " order by u.id desc",
            countQuery = "select count(u) from UserAccount u " + ADMIN_FILTER)
    Page<UserSummary> findAdminUsers(String search, String status, String role, Pageable page);

    @Query(ADMIN_USER + " where u.id = :id")
    Optional<UserSummary> findAdminUser(int id);

    @Query("""
            select u.id as userId, r.name as role from UserAccount u join u.roles r
            where u.id in :ids order by r.name
            """)
    List<UserRole> findRolesForUsers(List<Integer> ids);

    @Query("""
            select cast(u.createdAt as LocalDate) as day, count(u) as total
            from UserAccount u where u.createdAt >= :start
            group by cast(u.createdAt as LocalDate) order by day
            """)
    List<DailyCount> countNewAccounts(LocalDateTime start);

    interface UserSummary {
        int getId();
        String getUsername();
        String getEmail();
        Boolean getActive();
        LocalDateTime getCreatedAt();
        long getXp();
        int getCourseCount();
    }

    interface UserRole {
        int getUserId();
        String getRole();
    }
}
