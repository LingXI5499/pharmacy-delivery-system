package com.pharmacy.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service @RequiredArgsConstructor
public class AuditRecorder {
    private final BusinessAuditLogMapper mapper;
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void record(String traceId,Long actorId,String actorRole,String method,String path,int status,String ip){BusinessAuditLog row=new BusinessAuditLog();row.setTraceId(traceId);row.setActorId(actorId);row.setActorRole(actorRole);row.setEventType(method);row.setBusinessType(firstSegment(path));row.setBusinessId(path);row.setResult(status<400?"SUCCESS":"FAILED");row.setDetailsJson("{\"httpStatus\":"+status+"}");row.setClientIp(ip);row.setCreateTime(LocalDateTime.now());mapper.insert(row);}
    private static String firstSegment(String path){String[] p=path.split("/");return p.length>2?p[2].toUpperCase():"HTTP";}
}
