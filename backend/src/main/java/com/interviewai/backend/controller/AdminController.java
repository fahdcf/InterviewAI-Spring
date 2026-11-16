package com.interviewai.backend.controller;

import com.interviewai.backend.dto.response.AdminCourse;
import com.interviewai.backend.dto.response.AdminOverview;
import com.interviewai.backend.dto.response.AdminPage;
import com.interviewai.backend.dto.response.AdminUser;
import com.interviewai.backend.security.AccountPrincipal;
import com.interviewai.backend.service.AdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService admin;

    public AdminController(AdminService admin) {
        this.admin = admin;
    }

    @GetMapping("/users")
    public AdminPage<AdminUser> users(@RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "") String status, @RequestParam(defaultValue = "") String role,
            @RequestParam(defaultValue = "0") int page) {
        return admin.users(search, status, role, page);
    }

    @GetMapping("/users/roles")
    public List<String> roles() {
        return admin.roles();
    }

    @GetMapping("/users/{id}")
    public AdminUser user(@PathVariable int id) {
        return admin.user(id);
    }

    @PatchMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void editUser(@AuthenticationPrincipal AccountPrincipal actor, @PathVariable int id,
            @Valid @RequestBody UserEdit request) {
        admin.editUser(actor, id, request.email(), request.role(), request.active());
    }

    @PostMapping("/users/{id}/xp-reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetXp(@AuthenticationPrincipal AccountPrincipal actor, @PathVariable int id) {
        admin.reset(actor, id, false);
    }

    @PostMapping("/users/{id}/progress-reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetProgress(@AuthenticationPrincipal AccountPrincipal actor, @PathVariable int id) {
        admin.reset(actor, id, true);
    }

    @GetMapping("/courses")
    public AdminPage<AdminCourse> courses(@RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "") String status, @RequestParam(defaultValue = "0") int page) {
        return admin.courses(search, status, page);
    }

    @GetMapping("/courses/{id}")
    public AdminCourse course(@PathVariable int id) {
        return admin.course(id);
    }

    @PatchMapping("/courses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void editCourse(@PathVariable int id, @Valid @RequestBody CourseStatus status) {
        admin.changeCourse(id, status.status());
    }

    @DeleteMapping("/courses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCourse(@PathVariable int id) {
        admin.deleteCourse(id);
    }

    @GetMapping({"/overview", "/analytics"})
    public AdminOverview overview() {
        return admin.overview();
    }

    public record UserEdit(@Email @Size(max = 255) String email, @Size(max = 50) String role, Boolean active) {
    }

    public record CourseStatus(@NotBlank String status) {
    }
}
