package com.qc.template.service;

import com.qc.template.model.vo.HotTopicsVO;

public interface HotTopicService {

    HotTopicsVO getHotTopics();

    HotTopicsVO getHotTopics(boolean refresh);
}
