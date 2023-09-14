package com.chibi.springbootinit.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chibi.springbootinit.mapper.ChartMapper;
import com.chibi.springbootinit.model.entity.Chart;
import com.chibi.springbootinit.service.ChartService;
import org.springframework.stereotype.Service;

/**
* @author zc
* @description 针对表【chart(图标信息表)】的数据库操作Service实现
* @createDate 2023-07-27 15:32:05
*/
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
    implements ChartService{

}




