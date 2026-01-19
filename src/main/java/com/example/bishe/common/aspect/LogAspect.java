package com.example.bishe.common.aspect;

import com.example.bishe.common.BaseContext;
import com.example.bishe.common.annotation.Log;
import com.example.bishe.entity.OperationLog;

import com.example.bishe.entity.R;
import com.example.bishe.service.AsyncLogService;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//注解(标记) -> 切面(捕获) -> 反射(解析内容) -> 存储(结果)。

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {
    private final AsyncLogService asyncLogService;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Around("@annotation(controllerLog)")
    public Object doAround(ProceedingJoinPoint joinPoint, Log controllerLog) throws Throwable {
        //1.执行前，记录开始时间
        long startTime = System.currentTimeMillis();
        Object result=null;
        Exception exception=null;

        try{
            //执行原业务方法
            result = joinPoint.proceed();
            return result;
        }catch (Exception e){
            exception = e;
            throw e;
        }finally {
            //2.执行后，记录结束时间
            long costTime = System.currentTimeMillis() - startTime;

            //3.关键：在主线程里抓取所有“带不走”的数据
            //因为异步线程里拿不到RequestContext和ThreadLocal
            ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request=(attributes!=null)?attributes.getRequest():null;

            Long userId = BaseContext.getCurrentId();
            //4.组装对象-使用建造者模式
            OperationLog operLog = OperationLog.builder()
                    .operatorId(userId)
                    .title(controllerLog.title())
                    .businessType(String.valueOf(controllerLog.businessType()))
                    .costTime(costTime)
                    .status(exception==null?0:1)//0-成功，1-失败
                    .method(joinPoint.getSignature().getDeclaringTypeName()+"."+joinPoint.getSignature().getName())
                    .build();

            //设置请求相关信息（安全处理空指针）
            if(request != null){
                operLog.setOperIp(getClientIp(request));
                operLog.setOperUrl(request.getRequestURI());
                operLog.setRequestMethod(request.getMethod());
            }else{
                operLog.setOperIp("N/A");
                operLog.setOperUrl("N/A");
                operLog.setRequestMethod("N/A");
            }

            //记录异常
            if(exception != null){
                String errorMsg = exception.getMessage();
                if(errorMsg!=null&&errorMsg.length()>200){
                    errorMsg = errorMsg.substring(0, 200)+"...";
                }
                operLog.setErrorMsg(errorMsg);
            }

            //序列化请求参数(joinPoint.getArgs())
            try {
                processRequestParam(joinPoint, operLog);
            }catch (Exception jsonEx){
                log.warn("请求参数序列化失败，仅记录简单描述：{}", jsonEx.getMessage());
                operLog.setOperParam("参数处理异常");
            }

            //序列化返回结果
            try {
                if(result!=null){
                    processResponseResult(result,operLog);
                }
            }catch (Exception e){
                log.warn("返回结果序列化失败，仅记录简单描述：{}",e.getMessage());
                operLog.setJsonResult("结果处理异常");
            }
            //5.核心：调用异步Service的保存方法
            //主线程就直接回前端，写数据库的操作在异线程跑
            asyncLogService.saveLog(operLog);
        }
    }

    /**
     * 获取客户端IP地址（处理代理情况）
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时，第一个IP为真实IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }


    /**
     * 处理返回结果序列化
     */
    private void processRequestParam(ProceedingJoinPoint joinPoint, OperationLog operLog) throws Exception {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return;
        }
        List<Object> logArgs = new ArrayList<>();
        for (Object arg : args) {
            if (arg == null) continue;
            if (!(arg instanceof HttpServletRequest ||
                    arg instanceof HttpServletResponse ||
                    arg instanceof org.springframework.web.multipart.MultipartFile ||
                    arg instanceof org.springframework.validation.BindingResult ||
                    arg instanceof jakarta.servlet.ServletRequest ||
                    arg instanceof jakarta.servlet.ServletResponse)) {
                logArgs.add(objectMapper.writeValueAsString(arg));
            }
            if (!logArgs.isEmpty()) {
                String paramJson = objectMapper.writeValueAsString(logArgs);
                // 限制长度，避免数据库字段溢出
                if (paramJson.length() > 2000) {
                    paramJson = paramJson.substring(0, 2000) + "...(已截断)";
                }
                operLog.setOperParam(paramJson);
            }
        }
    }

    /**
     * 处理返回结果序列化 - 简化版
     */
    private void processResponseResult(Object result, OperationLog operLog) throws Exception {
        // 如果是R类型，简化处理
        if (result instanceof R) {
            R<?> r = (R<?>) result;
            Map<String, Object> simpleResult = new HashMap<>();
            simpleResult.put("code", r.getCode());
            simpleResult.put("msg", r.getMsg());


            // 只处理简单类型的data
            if (r.getData() != null) {
                if (r.getData() instanceof String ||
                        r.getData() instanceof Number ||
                        r.getData() instanceof Boolean ||
                        r.getData() instanceof Character) {
                    simpleResult.put("data", r.getData());
                } else {
                    // 复杂对象只记录类型和toString摘要
                    String dataStr = r.getData().toString();
                    if (dataStr.length() > 100) {
                        dataStr = dataStr.substring(0, 100) + "...";
                    }
                    simpleResult.put("dataSummary", dataStr);
                }
            }

            String resultJson = objectMapper.writeValueAsString(simpleResult);
            operLog.setJsonResult(resultJson);
        } else {
            // 其他类型直接序列化并限制长度
            String resultJson = objectMapper.writeValueAsString(result);
            if (resultJson.length() > 2000) {
                resultJson = resultJson.substring(0, 2000) + "...(已截断)";
            }
            operLog.setJsonResult(resultJson);
        }
    }
//    /**
//     * 处理返回结果序列化（利用反射版）
//     */
//    private void processResponseResult(Object result, OperationLog operLog) throws  Exception{
//        // 如果是R类型，简化处理
//        if(result instanceof R) {
//            R<?> r = (R<?>) result;
//            Map<String, Object> simpleResult = new HashMap<>();
//            simpleResult.put("code", r.getCode());
//            simpleResult.put("msg", r.getMsg());
//
//            //处理data字段 - 如果data是简单类型或可序列化，则包含
//            if (r.getData() != null) {
//                //对于简单的字符串或数值类型，直接包含
//                if(r.getData() instanceof String ||
//                        r.getData() instanceof Number||
//                        r.getData() instanceof Boolean){
//                    simpleResult.put("data", r.getData());
//                }else{
//                    //对于复杂对象，只记录类型信息
//                    simpleResult.put("dataType", r.getData().getClass().getSimpleName());
//
//                    //尝试简化复杂对象，只包含关键字段
//                    try{
//                        Map<String,Object> simplifiedData = simplifyDataObject(r.getData());
//                        if(!simplifiedData.isEmpty()){
//                            simpleResult.put("data", simplifiedData);
//                        }
//                    }catch (Exception e){
//                        //简化失败不处理
//                    }
//                }
//            }
//            String resultJson = objectMapper.writeValueAsString(simpleResult);
//            operLog.setJsonResult(resultJson);
//        }else{
//            // 其他类型直接序列化
//            String resultJson = objectMapper.writeValueAsString(result);
//            // 限制长度
//            if (resultJson.length() > 4000) {
//                resultJson = resultJson.substring(0, 4000) + "...(已截断)";
//            }
//            operLog.setJsonResult(resultJson);
//        }
//
//    }
//
//    /**
//     * 简化复杂对象，提取关键字段（利用反射）
//     */
//    private Map<String, Object> simplifyDataObject(Object data) throws Exception {
//        Map<String, Object> simplified = new HashMap<>();
//
//        // 如果是Map，简化处理
//        if (data instanceof Map) {
//            Map<?, ?> map = (Map<?, ?>) data;
//            int count = 0;
//            for (Map.Entry<?, ?> entry : map.entrySet()) {
//                if (count++ >= 5) break; // 只取前5个键值对
//                String key = String.valueOf(entry.getKey());
//                Object value = entry.getValue();
//
//                // 只处理简单的值类型
//                if (value instanceof String || value instanceof Number || value instanceof Boolean) {
//                    simplified.put(key, value);
//                } else if (value != null) {
//                    simplified.put(key, value.getClass().getSimpleName());
//                }
//            }
//            return simplified;
//        }
//
//        // 尝试使用反射获取常见字段
//        Class<?> clazz = data.getClass();
//        String[] commonFields = {"id", "name", "title", "code", "status"};
//
//        for (String fieldName : commonFields) {
//            try {
//                java.lang.reflect.Field field = clazz.getDeclaredField(fieldName);
//                field.setAccessible(true);
//                Object value = field.get(data);
//                if (value != null && (value instanceof String || value instanceof Number || value instanceof Boolean)) {
//                    simplified.put(fieldName, value);
//                }
//            } catch (NoSuchFieldException | IllegalAccessException e) {
//                // 字段不存在或不可访问，忽略
//            }
//        }
//
//        // 如果没有找到任何字段，记录类型信息
//        if (simplified.isEmpty()) {
//            simplified.put("type", clazz.getSimpleName());
//        }
//
//        return simplified;
//    }
}
