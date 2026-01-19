package com.example.bishe.service;

import com.example.bishe.entity.OperationLog;
import com.example.bishe.mapper.OperationLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncLogService {
    @Autowired
    private OperationLogMapper operationLogMapper;

    @Async //真正生效的异步注解
    public void saveLog(OperationLog log) {
        operationLogMapper.insert(log);
    }
}
