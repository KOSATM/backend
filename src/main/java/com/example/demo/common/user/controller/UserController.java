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
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.common.user.dto.User;
import com.example.demo.common.global.annotation.NoWrap;
import com.example.demo.common.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 사용자 관리 컨트롤러
// 사용자 CRUD 및 조회 기능을 제공합니다.
@RestController
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    // 사용자 목록 조회 (chat.html용 - 응답 래핑 제외)
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

    // 사용자 생성
    @PostMapping("/api/users")
    public ResponseEntity<User> createUser(@RequestBody User user) {
        log.info("사용자 생성 요청: email={}", user.getEmail());
        userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    // 사용자 단건 조회
    @GetMapping("/api/users/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    // 전체 사용자 목록 조회 (응답 래핑 포함)
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

    // 사용자 정보 수정
    @PutMapping("/api/users/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        userService.updateUserById(id, user);
        // 수정된 사용자 정보 조회하여 반환
        User updatedUser = userService.getUserById(id);
        return ResponseEntity.ok(updatedUser);
    }

    // 사용자 삭제 (실제 삭제)
    @DeleteMapping("/api/users/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        userService.deleteUserById(id);
        return ResponseEntity.ok("사용자가 삭제되었습니다.");
    }

    // 사용자 비활성화 (소프트 삭제)
    @PutMapping("/api/users/{id}/deactivate")
    public ResponseEntity<String> deactivateUser(@PathVariable Long id) {
        userService.deactivateUserById(id);
        return ResponseEntity.ok("사용자가 비활성화되었습니다.");
    }
}
