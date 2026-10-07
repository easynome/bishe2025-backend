package io.github.easynome.learnrecommend.service;

import io.github.easynome.learnrecommend.entity.OperationLog;

public interface AsyncLogService  {
    void saveLog(OperationLog operLog);

}
