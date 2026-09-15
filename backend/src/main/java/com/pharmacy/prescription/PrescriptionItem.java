package com.pharmacy.prescription;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data @TableName("prescription_item")
public class PrescriptionItem {
    @TableId(type=IdType.AUTO) private Long id;
    private Long prescriptionId; private Long medicineId; private Integer prescribedQty;
}
