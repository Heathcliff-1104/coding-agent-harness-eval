package com.koolearn.bms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.koolearn.bms.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
