package com.example.demo.common.user.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.common.dto.user.User;

// 사용자 데이터 접근 객체 (DAO)
// MyBatis를 통한 사용자 데이터베이스 작업을 처리합니다.
@Mapper
public interface UserDao {

    // 사용자 생성
    void insertUser(User user);

    // 사용자 ID로 조회
    User selectUserById(Long id);

    // 전체 사용자 목록 조회
    List<User> selectAllUsers();

    // 사용자 정보 수정
    void updateUser(User user);

    // 사용자 삭제
    void deleteUser(Long id);

    // 이메일로 사용자 조회
    User selectUserByEmail(String email);
}
