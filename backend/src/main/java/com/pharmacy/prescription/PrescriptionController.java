package com.pharmacy.prescription;

import com.pharmacy.common.ApiResponse;
import com.pharmacy.enums.UserRole;
import com.pharmacy.security.AuthenticatedUser;
import com.pharmacy.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController @RequiredArgsConstructor
public class PrescriptionController {
    private final PrescriptionService service;
    @PostMapping(value="/api/user/prescriptions",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Prescription> upload(@RequestPart MultipartFile file,@RequestParam List<Long> medicineIds,@RequestParam List<Integer> quantities){return ApiResponse.success(service.upload(CurrentUser.id(),file,medicineIds,quantities));}
    @GetMapping("/api/user/prescriptions/{id}/file") public ResponseEntity<Resource> userFile(@PathVariable Long id){return file(id,false);}
    @GetMapping("/api/pharmacist/prescriptions") @PreAuthorize("@permissionService.has('prescription.review')") public ApiResponse<List<Prescription>> pending(){return ApiResponse.success(service.pending());}
    @GetMapping("/api/pharmacist/prescriptions/{id}/file") public ResponseEntity<Resource> pharmacistFile(@PathVariable Long id){return file(id,true);}
    @PostMapping("/api/pharmacist/prescriptions/{id}/review") @PreAuthorize("@permissionService.has('prescription.review')") public ApiResponse<Void> review(@PathVariable Long id,@RequestBody ReviewRequest r){service.review(CurrentUser.id(),id,r.approved(),r.reason());return ApiResponse.success(null);}
    private ResponseEntity<Resource> file(Long id,boolean privileged){AuthenticatedUser u=CurrentUser.require();Prescription p=service.getAuthorized(id,u.id(),privileged||u.role()==UserRole.ADMIN||u.role()==UserRole.PHARMACIST);return ResponseEntity.ok().contentType(MediaType.parseMediaType(p.getContentType())).header(HttpHeaders.CONTENT_DISPOSITION,"inline; filename=prescription"+(p.getContentType().equals("application/pdf")?".pdf":".img")).body(service.resource(p));}
    public record ReviewRequest(boolean approved,String reason){}
}
