
package com.pharmacy.dto;
import jakarta.validation.constraints.Size;
import lombok.Data;
@Data
public class RemarkRequest { @Size(max = 255, message = "备注不能超过 255 个字符") private String adminRemark; }
