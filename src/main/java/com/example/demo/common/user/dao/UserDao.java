package com.example.demo.common.user.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.common.dto.user.User;

@Mapper
public interface UserDao {
    
    void insertUser(User user);
    
    User selectUserById(Long id);
    
    List<User> selectAllUsers();
    
    void updateUser(User user);
    
    void deleteUser(Long id);
    
    User selectUserByEmail(String email);
}
