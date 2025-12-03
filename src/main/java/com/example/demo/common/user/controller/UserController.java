package com.example.demo.common.user.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.common.dto.user.User;
import com.example.demo.common.global.annotation.NoWrap;
import com.example.demo.common.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@Slf4j
public class UserController {
    
    private final UserService userService;
    
    // For chat.html - simple /users endpoint
    @NoWrap
    @GetMapping("/users")
    public ResponseEntity<List<User>> getUsers() {
        try {
            List<User> users = userService.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("사용자 목록 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // REST API endpoints
    @PostMapping("/api/users")
    public ResponseEntity<User> createUser(@RequestBody User user) {
        try {
            log.info("사용자 생성 요청: email={}", user.getEmail());
            userService.createUser(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(user);
        } catch (Exception e) {
            log.error("사용자 생성 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/api/users/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        try {
            User user = userService.getUserById(id);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("사용자 조회 실패: id={}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/api/users")
    public ResponseEntity<List<User>> getAllUsers() {
        try {
            List<User> users = userService.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("전체 사용자 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PutMapping("/api/users/{id}")
    public ResponseEntity<Void> updateUser(@PathVariable Long id, @RequestBody User user) {
        try {
            boolean updated = userService.updateUserById(id, user);
            if (!updated) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("사용자 수정 실패: id={}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @DeleteMapping("/api/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        try {
            boolean deleted = userService.deleteUserById(id);
            if (!deleted) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("사용자 삭제 실패: id={}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
