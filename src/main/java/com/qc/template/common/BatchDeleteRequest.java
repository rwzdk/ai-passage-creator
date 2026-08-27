package com.qc.template.common;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class BatchDeleteRequest implements Serializable {

    private List<Long> ids;

    private static final long serialVersionUID = 1L;
}
