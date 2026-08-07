package com.yupi.template.service;

import com.yupi.template.model.vo.HotTopicsVO;

public interface HotTopicService {

    HotTopicsVO getHotTopics();

    HotTopicsVO getHotTopics(boolean refresh);
}
