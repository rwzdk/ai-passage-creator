package com.qc.template.service;

import com.qc.template.model.vo.StatisticsVO;

/**
 * 缁熻鏈嶅姟
 *
 */
public interface StatisticsService {

    /**
     * 鑾峰彇绯荤粺缁熻鏁版嵁
     *
     * @return 缁熻鏁版嵁
     */
    StatisticsVO getStatistics(boolean forceRefresh);
}
