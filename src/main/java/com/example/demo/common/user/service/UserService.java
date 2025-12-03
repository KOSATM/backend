package com.example.demo.common.user.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.common.dto.user.User;
import com.example.demo.common.user.dao.UserDao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    
    private final UserDao userDao;
    
    public void createUser(User user) {
        log.info("사용자 생성: email={}", user.getEmail());
        userDao.insertUser(user);
    }
    
    public User getUserById(Long id) {
        log.info("사용자 조회: id={}", id);
        return userDao.selectUserById(id);
    }
    
    public List<User> getAllUsers() {
        log.info("전체 사용자 조회");
        return userDao.selectAllUsers();
    }
    
    public boolean updateUserById(Long id, User user) {
        User existing = userDao.selectUserById(id);
        if (existing == null) {
            log.warn("사용자를 찾을 수 없음: id={}", id);
            return false;
        }
        user.setId(id);
        userDao.updateUser(user);
        log.info("사용자 수정 완료: id={}", id);
        return true;
    }
    
    public boolean deleteUserById(Long id) {
        User existing = userDao.selectUserById(id);
        if (existing == null) {
            log.warn("사용자를 찾을 수 없음: id={}", id);
            return false;
        }
        userDao.deleteUser(id);
        log.info("사용자 삭제 완료: id={}", id);
        return true;
    }
    
    public User getUserByEmail(String email) {
        return userDao.selectUserByEmail(email);
    }
}
