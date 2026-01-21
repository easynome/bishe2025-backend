package com.example.bishe.interceptor;

import com.example.bishe.common.BaseContext;
import com.example.bishe.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Configuration
public class JwtInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        //1.放行OPTIONS请求（跨域预检请求，前端框架如Vue/Axios会经常发）
        if("OPTIONS".equals(request.getMethod())){
            return true;
        }
        //2.从Header中获取Authorization
        String authHeader = request.getHeader("Authorization");

        //3.判断Authorization是否为空，或者格式是否正确
        if(authHeader==null||!authHeader.regionMatches(true,0,"Bearer ",0,7)){
            log.warn("鉴权失败：未发现有效的Token Header");
            throw new RuntimeException("未登录，请先登录");
        }
        String token = authHeader.substring(7);
        try{

            Long userId = JwtUtil.getUserIdFromToken(token);
            if(userId==null){
                throw new RuntimeException("Token解析无效");
            }
            //最重要一步：将解析出来的userId存入request域
            //这样后，在Controller中，就可以通过@RequestAttribute("currUserId") Long userId

            //相比BaseContext.getCurrentId()，setAttribute()只能在Controller中使用
            //而BaseContext可以在同一线程的任意地方获取当前用户ID
//            request.setAttribute("currUserId", userId);

            BaseContext.setCurrentId(userId);
            log.info("用户ID:{}鉴权通过，请访问路径：{}",userId,request.getRequestURI());
            return true;
        }catch (Exception e){
            log.error("Token解析异常：{}",e.getMessage());
            //这里抛出的异常会被GlobalExceptionHandler处理
            throw new RuntimeException("登录已过期或凭证错误，请重新登录");
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求执行完后，清理背包，防止影响下一个请求（线程复用）
        BaseContext.removeCurrentId();
        log.info("线程上下文已清理");
    }
}
