package com.example.demo.common.user.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.common.dto.user.User;

/**
 * 사용자 데이터 접근 객체 (DAO)
 * MyBatis를 통한 사용자 데이터베이스 작업을 처리합니다.
 */
@Mapper
public interface UserDao {

    /**
     * 사용자 생성
     * @param user 생성할 사용자 정보
     */
    void insertUser(User user);

    /**
     * 사용자 ID로 조회
     * @param id 조회할 사용자 ID
     * @return 사용자 정보
     */
    User selectUserById(Long id);

    /**
     * 전체 사용자 목록 조회
     * @return 전체 사용자 목록
     */
    List<User> selectAllUsers();

    /**
     * 사용자 정보 수정
     * @param user 수정할 사용자 정보
     */
    void updateUser(User user);

    /**
     * 사용자 삭제
     * @param id 삭제할 사용자 ID
     */
    void deleteUser(Long id);

    /**
     * 이메일로 사용자 조회
     * @param email 조회할 이메일
     * @return 사용자 정보
     */
    User selectUserByEmail(String email);
}
