package com.example.demo.common.user.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.common.user.dto.User;
import com.example.demo.common.user.dao.UserDao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 사용자 서비스
// 사용자 관련 비즈니스 로직을 처리합니다.
@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserDao userDao;

    // 사용자 생성
    public void createUser(User user) {
        log.info("사용자 생성: email={}", user.getEmail());

        // 이메일 중복 체크
        User existing = userDao.selectUserByEmail(user.getEmail());
        if (existing != null) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다: " + user.getEmail());
        }

        userDao.insertUser(user);
    }

    // 사용자 ID로 조회
    public User getUserById(Long id) {
        log.info("사용자 조회: id={}", id);
        User user = userDao.selectUserById(id);
        if (user == null) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다: id=" + id);
        }
        return user;
    }

    // 전체 사용자 목록 조회
    public List<User> getAllUsers() {
        log.info("전체 사용자 조회");
        return userDao.selectAllUsers();
    }

    // 사용자 정보 수정
    public void updateUserById(Long id, User user) {
        User existing = userDao.selectUserById(id);
        if (existing == null) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다: id=" + id);
        }
        user.setId(id);
        userDao.updateUser(user);
        log.info("사용자 수정 완료: id={}", id);
    }

    // 사용자 삭제 (실제 삭제)
    public void deleteUserById(Long id) {
        User existing = userDao.selectUserById(id);
        if (existing == null) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다: id=" + id);
        }
        userDao.deleteUser(id);
        log.info("사용자 삭제 완료: id={}", id);
    }

    // 사용자 비활성화 (소프트 삭제)
    public void deactivateUserById(Long id) {
        User existing = userDao.selectUserById(id);
        if (existing == null) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다: id=" + id);
        }
        userDao.deactivateUser(id);
        log.info("사용자 비활성화 완료: id={}", id);
    }

    // 이메일로 사용자 조회
    public User getUserByEmail(String email) {
        return userDao.selectUserByEmail(email);
    }
}
