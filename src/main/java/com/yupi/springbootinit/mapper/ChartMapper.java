package com.yupi.springbootinit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yupi.springbootinit.model.entity.Chart;
import org.springframework.stereotype.Component;

/**
* @author zc
* @description 针对表【chart(图标信息表)】的数据库操作Mapper
* @createDate 2023-07-27 15:32:05
* @Entity com.yupi.springbootinit.model.entity.Chart
*/
@Component
public interface ChartMapper extends BaseMapper<Chart> {

}




