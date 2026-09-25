package com.ecommerce.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.product.entity.StockOperation;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface StockOperationMapper extends BaseMapper<StockOperation> {

    @Select("SELECT COUNT(1) FROM t_stock_operation WHERE biz_no = #{bizNo} AND op_type = #{opType}")
    int countByBizNoAndType(@Param("bizNo") String bizNo, @Param("opType") String opType);
}
