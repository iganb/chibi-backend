package com.yupi.springbootinit.controller;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;
import com.yupi.springbootinit.annotation.AuthCheck;
import com.yupi.springbootinit.bizmq.BiMessageProducer;
import com.yupi.springbootinit.common.BaseResponse;
import com.yupi.springbootinit.common.DeleteRequest;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.common.ResultUtils;
import com.yupi.springbootinit.constant.CommonConstant;
import com.yupi.springbootinit.constant.FileConstant;
import com.yupi.springbootinit.constant.UserConstant;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.exception.ThrowUtils;
import com.yupi.springbootinit.manager.AiManager;
import com.yupi.springbootinit.manager.RedisLimitManager;
import com.yupi.springbootinit.model.dto.chart.*;
import com.yupi.springbootinit.model.dto.file.UploadFileRequest;
import com.yupi.springbootinit.model.dto.post.PostQueryRequest;
import com.yupi.springbootinit.model.entity.Chart;
import com.yupi.springbootinit.model.entity.Post;
import com.yupi.springbootinit.model.entity.User;
import com.yupi.springbootinit.model.enums.FileUploadBizEnum;
import com.yupi.springbootinit.model.vo.BIResponse;
import com.yupi.springbootinit.service.ChartService;
import com.yupi.springbootinit.service.UserService;
import com.yupi.springbootinit.utils.ExcelUtils;
import com.yupi.springbootinit.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 帖子接口
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Resource
    private AiManager aiManager;


    @Resource
    private ThreadPoolExecutor threadPoolExecutor;
    @Resource
    private RedisLimitManager redisLimitManager;

    @Resource
    private BiMessageProducer biMessageProducer;



    // region 增删改查

    /**
     * 创建
     *
     * @param chartAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addchart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);

        User loginUser = userService.getLoginUser(request);
        chart.setUserId(loginUser.getId());

        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newchartId = chart.getId();
        return ResultUtils.success(newchartId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Chart oldchart = chartService.getById(id);
        ThrowUtils.throwIf(oldchart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldchart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param chartUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);

        long id = chartUpdateRequest.getId();
        // 判断是否存在
        Chart oldchart = chartService.getById(id);
        ThrowUtils.throwIf(oldchart == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Chart> getChartById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(chart);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
            HttpServletRequest request) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
               getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<Chart>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
            HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    // endregion
    /**
     * 智能分析
     *
     * @param multipartFile
     * @param genChartByAIRequest
     * @param request
     * @return
     */
    @PostMapping("/gen")
    public BaseResponse<BIResponse> genChartByAI(@RequestPart("file") MultipartFile multipartFile,
                                             GenChartByAIRequest genChartByAIRequest, HttpServletRequest request) {

        String name = genChartByAIRequest.getName();
        String goal = genChartByAIRequest.getGoal();
        String chartType = genChartByAIRequest.getChartType();
        //校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal),ErrorCode.PARAMS_ERROR,"目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length()>100,ErrorCode.PARAMS_ERROR,"名称过长");
        //校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        final  long ONE_MB= 1024*1024;

        ThrowUtils.throwIf(size>ONE_MB,ErrorCode.PARAMS_ERROR,"文件超过1M");
        //校验文件后缀 aaa.png
        String suffix = FileUtil.getSuffix(originalFilename);
        final  List<String> validFileSuffix= Arrays.asList("xlsx","xls");
        ThrowUtils.throwIf(!validFileSuffix.contains(suffix),ErrorCode.PARAMS_ERROR,"文件后缀非法");

        User loginUser = userService.getLoginUser(request);

        //限流判断
        redisLimitManager.doRateLimit("genChartByAI_"+loginUser.getId());


        long biModelId=1679305672904269825L;
//        分析需求：
//        分析网站用户的增长情况
//        原始数据：
//        日期，用户数
//        1号，10
//        2号，30
//        3号，50

        //构造用户输入
        StringBuilder userInput=new StringBuilder();
        userInput.append("分析需求：").append("\n");
        //拼接分析目标
        String userGoal=goal;
//        if (StringUtils.isNotBlank(userGoal)){
//            userGoal+="/请使用"+chartType;
//        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据：").append("\n");
        //压缩后的数据
        String csvData=ExcelUtils.excelToCsv(multipartFile);

        userInput.append(csvData).append("\n");

        //插入到数据库
        Chart chart = new Chart();

        chart.setGoal(userGoal);
        chart.setName(name);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setStatus("wait");
        chart.setUserId(loginUser.getId());
        boolean saveResult=chartService.save(chart);
        ThrowUtils.throwIf(!saveResult,ErrorCode.SYSTEM_ERROR,"图表保存失败");


        CompletableFuture.runAsync(()->{
// 先修改图表任务状态为 “执行中”。等执行成功后，修改为 “已完成”、保存执行结果；执行失败后，状态修改为 “失败”，记录任务失败信息。(为了防止同一个任务被多次执行)
            Chart updateChart =new Chart();
            updateChart.setId(chart.getId());
            // 把任务状态改为执行中
            updateChart.setStatus("running");
            boolean b = chartService.updateById(updateChart);
            // 如果提交失败(一般情况下,更新失败可能意味着你的数据库出问题了)
            if (!b){
               handleChartUpdateError(chart.getId(),"更新图表执行中状态失败");
               return;
            }

            //调用AI
            String result = aiManager.doChat(biModelId, userInput.toString());
            String[] splits=result.split("【【【【【");
            if (splits.length<3){
                handleChartUpdateError(chart.getId(),"AI生成错误");
                return;
            }
            String genChart=splits[1].trim();
            String genResult=splits[2].trim();
            // 调用AI得到结果之后,再更新一次
            Chart updateChartResult =new Chart();
            updateChartResult.setId(chart.getId());
            updateChartResult.setGenChart(genChart);
            updateChartResult.setGenResult(genResult);
            updateChartResult.setStatus("succeed");
            boolean updateResult = chartService.updateById(updateChartResult);
            if (!updateResult){
                handleChartUpdateError(chart.getId(),"更新图表成功状态失败");

            }
        },threadPoolExecutor);
        BIResponse biResponse = new BIResponse();
        biResponse.setChartId(chart.getId());


        
        return ResultUtils.success(biResponse);

    }

    /**
     * 智能分析（异步消息队列）
     * @param multipartFile
     * @param genChartByAIRequest
     * @param request
     * @return
     */
    @PostMapping("/gen/async/mq")
    public BaseResponse<BIResponse> genChartByAIAsyncMq(@RequestPart("file") MultipartFile multipartFile,
                                                 GenChartByAIRequest genChartByAIRequest, HttpServletRequest request) {

        String name = genChartByAIRequest.getName();
        String goal = genChartByAIRequest.getGoal();
        String chartType = genChartByAIRequest.getChartType();
        //校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal),ErrorCode.PARAMS_ERROR,"目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length()>100,ErrorCode.PARAMS_ERROR,"名称过长");
        //校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        final  long ONE_MB= 1024*1024;

        ThrowUtils.throwIf(size>ONE_MB,ErrorCode.PARAMS_ERROR,"文件超过1M");
        //校验文件后缀 aaa.png
        String suffix = FileUtil.getSuffix(originalFilename);
        final  List<String> validFileSuffix= Arrays.asList("xlsx","xls");
        ThrowUtils.throwIf(!validFileSuffix.contains(suffix),ErrorCode.PARAMS_ERROR,"文件后缀非法");

        User loginUser = userService.getLoginUser(request);

        //限流判断
        redisLimitManager.doRateLimit("genChartByAI_"+loginUser.getId());
        //无需写prompt
//        final  String prompt="你是一个数据分析师和前端开发专家，接下来我会按照一下固定格式给你提供内容：\n" +
//                "分析需求：\n" +
//                "{数据分析的需求或目标}\n" +
//                "原始数据：\n" +
//                "{csv格式的原始数据，用,作为分隔符}\n" +
//                "请根据以上内容，按照以下指定格式生成内容（此外不要输出多余的开头、结尾、注释）\n" +
//                "【【【【【\n" +
//                "{前端Echarts V5的option配置对象js代码，合理地将数据可视化，不要生成任何多余的内容，比如注释}\n" +
//                "【【【【【\n" +
//                "{明确的数据分析结论、越详细越好，不要生成多余的注释}";

        long biModelId=CommonConstant.BI_MODEL_ID;
//        分析需求：
//        分析网站用户的增长情况
//        原始数据：
//        日期，用户数
//        1号，10
//        2号，30
//        3号，50

        //构造用户输入
        StringBuilder userInput=new StringBuilder();
        userInput.append("分析需求：").append("\n");
        //拼接分析目标
        String userGoal=goal;
//        if (StringUtils.isNotBlank(userGoal)){
//            userGoal+="/请使用"+chartType;
//        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据：").append("\n");
        //压缩后的数据
        String csvData=ExcelUtils.excelToCsv(multipartFile);

        userInput.append(csvData).append("\n");

        //插入到数据库
        Chart chart = new Chart();

        chart.setGoal(userGoal);
        chart.setName(name);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setStatus("wait");
        chart.setUserId(loginUser.getId());
        boolean saveResult=chartService.save(chart);
        ThrowUtils.throwIf(!saveResult,ErrorCode.SYSTEM_ERROR,"图表保存失败");


        CompletableFuture.runAsync(()->{
            // 先修改图表任务状态为 “执行中”。等执行成功后，修改为 “已完成”、保存执行结果；执行失败后，状态修改为 “失败”，记录任务失败信息。(为了防止同一个任务被多次执行)
            Chart updateChart =new Chart();
            updateChart.setId(chart.getId());
            // 把任务状态改为执行中
            updateChart.setStatus("running");
            boolean b = chartService.updateById(updateChart);
            // 如果提交失败(一般情况下,更新失败可能意味着你的数据库出问题了)
            if (!b){
                handleChartUpdateError(chart.getId(),"更新图表执行中状态失败");
                return;
            }

            //调用AI
            String result = aiManager.doChat(biModelId, userInput.toString());
            String[] splits=result.split("【【【【【");
            if (splits.length<3){
                handleChartUpdateError(chart.getId(),"AI生成错误");
                return;
            }
            String genChart=splits[1].trim();
            String genResult=splits[2].trim();
            // 调用AI得到结果之后,再更新一次
            Chart updateChartResult =new Chart();
            updateChartResult.setId(chart.getId());
            updateChartResult.setGenChart(genChart);
            updateChartResult.setGenResult(genResult);
            updateChartResult.setStatus("succeed");
            boolean updateResult = chartService.updateById(updateChartResult);
            if (!updateResult){
                handleChartUpdateError(chart.getId(),"更新图表成功状态失败");

            }
        },threadPoolExecutor);
        BIResponse biResponse = new BIResponse();
        biResponse.setChartId(chart.getId());



        return ResultUtils.success(biResponse);

    }
    private void handleChartUpdateError(long chartId,String execMessage){
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chartId);
        updateChartResult.setStatus("failed");
        updateChartResult.setExecMessage(execMessage);
        boolean updateResult = chartService.updateById(updateChartResult);
        if (!updateResult){
           log.error("更新图表失败状态失败："+chartId+","+execMessage);
        }
    }

    /**
     * 智能分析（异步线程池）
     * @param multipartFile
     * @param genChartByAIRequest
     * @param request
     * @return
     */
    @PostMapping("/gen/async")
    public BaseResponse<BIResponse> genChartByAIAsync(@RequestPart("file") MultipartFile multipartFile,
                                                      GenChartByAIRequest genChartByAIRequest, HttpServletRequest request) {

        String name = genChartByAIRequest.getName();
        String goal = genChartByAIRequest.getGoal();
        String chartType = genChartByAIRequest.getChartType();
        //校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal),ErrorCode.PARAMS_ERROR,"目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length()>100,ErrorCode.PARAMS_ERROR,"名称过长");
        //校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        final  long ONE_MB= 1024*1024;

        ThrowUtils.throwIf(size>ONE_MB,ErrorCode.PARAMS_ERROR,"文件超过1M");
        //校验文件后缀 aaa.png
        String suffix = FileUtil.getSuffix(originalFilename);
        final  List<String> validFileSuffix= Arrays.asList("xlsx","xls");
        ThrowUtils.throwIf(!validFileSuffix.contains(suffix),ErrorCode.PARAMS_ERROR,"文件后缀非法");

        User loginUser = userService.getLoginUser(request);

        //限流判断
        redisLimitManager.doRateLimit("genChartByAI_"+loginUser.getId());


        long biModelId=1679305672904269825L;
//        分析需求：
//        分析网站用户的增长情况
//        原始数据：
//        日期，用户数
//        1号，10
//        2号，30
//        3号，50

        //构造用户输入
        StringBuilder userInput=new StringBuilder();
        userInput.append("分析需求：").append("\n");
        //拼接分析目标
        String userGoal=goal;
//        if (StringUtils.isNotBlank(userGoal)){
//            userGoal+="/请使用"+chartType;
//        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据：").append("\n");
        //压缩后的数据
        String csvData=ExcelUtils.excelToCsv(multipartFile);

        userInput.append(csvData).append("\n");

        //插入到数据库
        Chart chart = new Chart();

        chart.setGoal(userGoal);
        chart.setName(name);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setStatus("wait");
        chart.setUserId(loginUser.getId());
        boolean saveResult=chartService.save(chart);
        ThrowUtils.throwIf(!saveResult,ErrorCode.SYSTEM_ERROR,"图表保存失败");


        CompletableFuture.runAsync(()->{

        },threadPoolExecutor);
        BIResponse biResponse = new BIResponse();
        biResponse.setChartId(chart.getId());



        return ResultUtils.success(biResponse);

    }

    /**
     * 编辑（用户）
     *
     * @param chartEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editChart(@RequestBody ChartEditRequest chartEditRequest, HttpServletRequest request) {
        if (chartEditRequest == null || chartEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartEditRequest, chart);

        User loginUser = userService.getLoginUser(request);
        long id = chartEditRequest.getId();
        // 判断是否存在
        Chart oldchart = chartService.getById(id);
        ThrowUtils.throwIf(oldchart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldchart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }
    /**
     * 获取查询包装类
     *
     * @param chartQueryRequest
     * @return
     */

    public QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }

        Long id = chartQueryRequest.getId();
        String name = chartQueryRequest.getName();
        String goal = chartQueryRequest.getGoal();
        String chartType = chartQueryRequest.getTags();
        Long userId = chartQueryRequest.getUserId();

        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();


        queryWrapper.eq(id !=null && id>0,"id",id);
        queryWrapper.like(StringUtils.isNotBlank(name),"name",name);
        queryWrapper.eq(StringUtils.isNotBlank(goal),"goal",goal);
        queryWrapper.eq(StringUtils.isNotBlank(chartType),"goal",chartType);


        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }
}
