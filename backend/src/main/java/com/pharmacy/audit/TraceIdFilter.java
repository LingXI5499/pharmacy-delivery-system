package com.pharmacy.audit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import com.pharmacy.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.UUID;

@Component @RequiredArgsConstructor @Slf4j
public class TraceIdFilter extends OncePerRequestFilter {
    private final AuditRecorder audit;
    public static final String HEADER="X-Trace-Id";
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
        String supplied=request.getHeader(HEADER);String traceId=supplied!=null&&supplied.matches("[A-Za-z0-9_-]{8,64}")?supplied:UUID.randomUUID().toString();
        MDC.put("traceId",traceId);response.setHeader(HEADER,traceId);try{chain.doFilter(request,response);}finally{if(!"GET".equals(request.getMethod())&&!"OPTIONS".equals(request.getMethod())){try{Object p=request.getAttribute(AuthenticatedUser.class.getName());AuthenticatedUser u=p instanceof AuthenticatedUser a?a:null;audit.record(traceId,u==null?null:u.id(),u==null?null:u.role().name(),request.getMethod(),request.getRequestURI(),response.getStatus(),request.getRemoteAddr());}catch(RuntimeException e){log.error("audit persistence failed traceId={}",traceId,e);}}MDC.remove("traceId");}
    }
}
