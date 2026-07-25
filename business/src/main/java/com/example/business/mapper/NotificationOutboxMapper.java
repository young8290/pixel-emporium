package com.example.business.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.business.entity.NotificationOutbox;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import java.time.LocalDateTime;
public interface NotificationOutboxMapper extends BaseMapper<NotificationOutbox> {
 @Update("update notification_outbox set status='SENDING', claimed_at=#{now}, attempts=attempts+1 where id=#{id} and status in ('PENDING','RETRY') and (next_retry_at is null or next_retry_at <= #{now}) and deleted=0") int claim(@Param("id") Long id, @Param("now") LocalDateTime now);
 @Update("update notification_outbox set status='SENT', delivered_at=#{deliveredAt}, claimed_at=null, next_retry_at=null, last_error=null where id=#{id} and status='SENDING' and deleted=0") int markSent(@Param("id") Long id, @Param("deliveredAt") LocalDateTime deliveredAt);
 @Update("update notification_outbox set status=#{status}, claimed_at=null, next_retry_at=#{nextRetryAt}, last_error=#{lastError} where id=#{id} and status='SENDING' and deleted=0") int recordFailure(@Param("id") Long id, @Param("status") String status, @Param("nextRetryAt") LocalDateTime nextRetryAt, @Param("lastError") String lastError);
 @Update("update notification_outbox set status='RETRY', claimed_at=null, next_retry_at=#{now}, last_error='ClaimTimeout' where status='SENDING' and claimed_at < #{staleBefore} and deleted=0") int recoverStaleClaims(@Param("now") LocalDateTime now, @Param("staleBefore") LocalDateTime staleBefore);
}
