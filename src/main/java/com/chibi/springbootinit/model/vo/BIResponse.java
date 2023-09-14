package com.chibi.springbootinit.model.vo;

import io.swagger.models.auth.In;
import lombok.Data;

/**
 * BI返回结果
 */
@Data
public class BIResponse {

    private Long chartId;

    private  String genChart;
    private String genResult;
}
