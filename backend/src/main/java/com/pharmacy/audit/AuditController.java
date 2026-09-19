package com.pharmacy.audit;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pharmacy.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController @RequestMapping("/api/admin/audit-logs") @RequiredArgsConstructor
public class AuditController {
    private final BusinessAuditLogMapper mapper;
    @GetMapping @PreAuthorize("@permissionService.has('audit.read')") public ApiResponse<List<BusinessAuditLog>> query(@RequestParam(required=false)Long actorId,@RequestParam(required=false)String businessId,@RequestParam(required=false)String eventType,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)LocalDateTime start,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)LocalDateTime end){return ApiResponse.success(mapper.selectList(new LambdaQueryWrapper<BusinessAuditLog>().eq(actorId!=null,BusinessAuditLog::getActorId,actorId).like(businessId!=null&&!businessId.isBlank(),BusinessAuditLog::getBusinessId,businessId).eq(eventType!=null&&!eventType.isBlank(),BusinessAuditLog::getEventType,eventType).ge(start!=null,BusinessAuditLog::getCreateTime,start).le(end!=null,BusinessAuditLog::getCreateTime,end).orderByDesc(BusinessAuditLog::getCreateTime).last("LIMIT 500")));}
}
