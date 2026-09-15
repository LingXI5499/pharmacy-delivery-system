package com.pharmacy.messaging;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data @TableName("inbox_event")
public class InboxEvent {
    @TableId(type=IdType.AUTO) private Long id;
    private String consumerName; private String eventId; private String eventType; private LocalDateTime processedTime;
}
