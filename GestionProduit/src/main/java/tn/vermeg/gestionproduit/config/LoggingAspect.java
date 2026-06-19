package tn.vermeg.gestionproduit.config;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
@Aspect
@Component
public class LoggingAspect {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Pointcut("within(tn.vermeg.gestionproduit.services..*) || within(tn.vermeg.gestionproduit.controllers..*)")
    public void applicationPackagePointcut() {}
    @Before("applicationPackagePointcut()")
    public void logBefore(JoinPoint joinPoint) {
        logger.info("[AUDIT] Entering method: {}.{}() with arguments = {}", 
            joinPoint.getSignature().getDeclaringTypeName(), 
            joinPoint.getSignature().getName(), 
            Arrays.toString(joinPoint.getArgs()));}
    @AfterReturning(pointcut = "applicationPackagePointcut()", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, Object result) {
        logger.info("[AUDIT] Exiting method: {}.{}() with result = {}", 
            joinPoint.getSignature().getDeclaringTypeName(), 
            joinPoint.getSignature().getName(), 
            result);}
}