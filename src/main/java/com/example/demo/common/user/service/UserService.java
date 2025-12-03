package com.example.demo.common.user.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.common.dto.user.User;
import com.example.demo.common.user.dao.UserDao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 사용자 서비스
 * 사용자 관련 비즈니스 로직을 처리합니다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    
    private final UserDao userDao;
    
    /**
     * 사용자 생성
     * 
     * @param user 생성할 사용자 정보
     */
    public void createUser(User user) {
        log.info("사용자 생성: email={}", user.getEmail());
        userDao.insertUser(user);
    }
    
    /**
     * 사용자 ID로 조회
     * 
     * @param id 조회할 사용자 ID
     * @return 사용자 정보, 없으면 null
     */
    public User getUserById(Long id) {
        log.info("사용자 조회: id={}", id);
        return userDao.selectUserById(id);
    }
    
    /**
     * 전체 사용자 목록 조회
     * 
     * @return 전체 사용자 목록
     */
    public List<User> getAllUsers() {
        log.info("전체 사용자 조회");
        return userDao.selectAllUsers();
    }
    
    /**
     * 사용자 정보 수정
     * 
     * @param id 수정할 사용자 ID
     * @param user 수정할 사용자 정보
     * @return 수정 성공 여부
     */
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
    
    /**
     * 사용자 삭제
     * 
     * @param id 삭제할 사용자 ID
     * @return 삭제 성공 여부
     */
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
    
    /**
     * 이메일로 사용자 조회
     * 
     * @param email 조회할 이메일
     * @return 사용자 정보, 없으면 null
     */
    public User getUserByEmail(String email) {
        return userDao.selectUserByEmail(email);
    }
}
