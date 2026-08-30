package com.qc.template.mapper;

import com.mybatisflex.core.BaseMapper;
import com.qc.template.model.entity.PaymentRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付记录 Mapper
 *
 */
@Mapper
public interface PaymentRecordMapper extends BaseMapper<PaymentRecord> {
}
