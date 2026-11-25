package com.interviewai.backend.repository;

import com.interviewai.backend.model.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {

    Optional<Role> findByName(String name);

    @Query("select r.name from Role r order by r.name")
    List<String> findNames();
}
