package com.chibi.springbootinit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chibi.springbootinit.model.entity.Chart;
import org.springframework.stereotype.Component;

/**
* @author zc
* @description 针对表【chart(图标信息表)】的数据库操作Mapper
* @createDate 2023-07-27 15:32:05
* @Entity entity.model.com.chibi.springbootinit.Chart
*/
@Component
public interface ChartMapper extends BaseMapper<Chart> {

}




