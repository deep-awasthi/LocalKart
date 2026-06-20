package com.localkart.platform.shared.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *) || within(@org.springframework.stereotype.Service *)")
    public void applicationPackagePointcut() {
        // Pointcut definition for REST Controllers and Services
    }

    @Around("applicationPackagePointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        
        log.debug("Entering: {}.{}() with arguments = {}", className, methodName, Arrays.toString(joinPoint.getArgs()));
        
        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - start;
            
            log.debug("Exiting: {}.{}() with result = {}. Execution time: {} ms", className, methodName, result, executionTime);
            if (executionTime > 500) {
                log.warn("Slow execution detected: {}.{}() took {} ms", className, methodName, executionTime);
            }
            return result;
        } catch (Throwable e) {
            log.error("Exception in {}.{}() with message = {}", className, methodName, e.getMessage());
            throw e;
        }
    }
}
