package com.example.business.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.common.orm.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("user_browse_history")
public class BrowseHistory extends BaseEntity {
    private Long userId;
    private Long productId;
    private Integer viewCount;
    private LocalDateTime lastViewedAt;
}
