package tn.vermeg.gestionuser.config;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
public class LoggingAspect {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Pointcut("within(tn.vermeg.gestionuser.services..*) || within(tn.vermeg.gestionuser.controllers..*)")
    public void applicationPackagePointcut() {}
    @Before("applicationPackagePointcut()")
    public void logBefore(JoinPoint joinPoint) {
        String currentUser = SecurityContextHolder.getContext().getAuthentication() != null 
            ? SecurityContextHolder.getContext().getAuthentication().getName() 
            : "System";
        logger.info("[AUDIT] User: {} | Entering method: {}.{}() with arguments = {}", 
            currentUser,
            joinPoint.getSignature().getDeclaringTypeName(), 
            joinPoint.getSignature().getName(), 
            Arrays.toString(joinPoint.getArgs()));}

    @AfterReturning(pointcut = "applicationPackagePointcut()", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, Object result) {
        String currentUser = SecurityContextHolder.getContext().getAuthentication() != null 
            ? SecurityContextHolder.getContext().getAuthentication().getName() 
            : "System";
        logger.info("[AUDIT] User: {} | Exiting method: {}.{}() with result = {}", 
            currentUser,
            joinPoint.getSignature().getDeclaringTypeName(), 
            joinPoint.getSignature().getName(), 
            result);}
}