package com.example.business.service;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.example.business.entity.NotificationOutbox;
import com.example.business.enums.NotificationOutboxStatus;
import com.example.business.mapper.NotificationOutboxMapper;
import org.springframework.stereotype.Service;
@Service public class NotificationOutboxService { private final NotificationOutboxMapper mapper; public NotificationOutboxService(NotificationOutboxMapper mapper){this.mapper=mapper;} public void appendOrderCreated(Long orderId){ NotificationOutbox row=new NotificationOutbox(); row.setId(IdWorker.getId()); row.setEventId("ORDER_CREATED:"+orderId); row.setEventType("ORDER_CREATED"); row.setAggregateId(orderId); row.setStatus(NotificationOutboxStatus.PENDING); row.setAttempts(0); if(mapper.insert(row)!=1) throw new IllegalStateException("订单通知事件保存失败"); } }
