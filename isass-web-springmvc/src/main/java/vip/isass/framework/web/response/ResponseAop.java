// SPDX-License-Identifier: LGPL-3.0-only

//package vip.isass.framework.web.response;
//
//import lombok.extern.slf4j.Slf4j;
//import org.aspectj.lang.ProceedingJoinPoint;
//import org.aspectj.lang.annotation.Around;
//import org.aspectj.lang.annotation.Aspect;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Component;
//import vip.isass.framework.web.Resp;
//
///**
// * @author Rain
// * 请求日志记录器
// */
//@Slf4j
//@Aspect
//@Component
//public class ResponseAop {
//
//    public Object requestLog(ProceedingJoinPoint joinPoint) throws Throwable {
//        Object proceed = joinPoint.proceed();
//
//        if (proceed == null
//            || proceed instanceof Resp
//            || proceed instanceof ResponseEntity) {
//            return proceed;
//        }
//
//        return Resp.bizSuccess(proceed);
//    }
//
//}
