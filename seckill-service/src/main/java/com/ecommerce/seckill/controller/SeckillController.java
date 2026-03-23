package com.ecommerce.seckill.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.seckill.entity.SeckillActivity;
import com.ecommerce.seckill.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 秒杀控制器
 */
@Slf4j
@RestController
@RequestMapping("/seckill")
public class SeckillController {

    @Resource
    private SeckillService seckillService;

    /**
     * 查询进行中的秒杀活动列表
     */
    @GetMapping("/active-list")
    public Result<List<SeckillActivity>> getActiveList() {
        List<SeckillActivity> list = seckillService.getActiveList();
        return Result.success(list);
    }

    /**
     * 查询秒杀活动详情
     */
    @GetMapping("/detail/{activityId}")
    public Result<SeckillActivity> getActivityDetail(@PathVariable Long activityId) {
        SeckillActivity activity = seckillService.getActivityById(activityId);
        return Result.success(activity);
    }

    /**
     * 秒杀库存预热（管理员接口）
     */
    @PostMapping("/warm-up/{activityId}")
    public Result<?> warmUpStock(@PathVariable Long activityId) {
        seckillService.warmUpSeckillStock(activityId);
        return Result.success();
    }

    /**
     * 执行秒杀（核心接口）
     */
    @PostMapping("/do-seckill")
    public Result<String> doSeckill(@RequestParam Long activityId,
                                    @RequestParam Integer quantity,
                                    @RequestHeader("userId") Long userId) {
        String orderNo = seckillService.doSeckill(activityId, userId, quantity);
        return Result.success(orderNo);
    }
}



