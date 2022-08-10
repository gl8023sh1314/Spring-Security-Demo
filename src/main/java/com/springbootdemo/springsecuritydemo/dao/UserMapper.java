package com.springbootdemo.springsecuritydemo.dao;


import com.springbootdemo.springsecuritydemo.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {
    User selectByName(String username);
}
