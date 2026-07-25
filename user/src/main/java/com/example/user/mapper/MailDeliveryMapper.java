package com.example.user.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.user.entity.MailDelivery;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

public interface MailDeliveryMapper extends BaseMapper<MailDelivery> {
    @Update("update mail_delivery set status = 'SENDING', claimed_at = #{now}, attempts = attempts + 1 "
            + "where id = #{id} and status in ('PENDING', 'RETRY') "
            + "and (next_retry_at is null or next_retry_at <= #{now}) and deleted = 0")
    int claim(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Update("update mail_delivery set status = 'RETRY', claimed_at = null, next_retry_at = #{now}, "
            + "last_error = 'ClaimTimeout' where status = 'SENDING' and claimed_at < #{staleBefore} "
            + "and deleted = 0")
    int recoverStaleClaims(@Param("now") LocalDateTime now, @Param("staleBefore") LocalDateTime staleBefore);

    @Update("update mail_delivery set status = 'SENT', sent_at = #{sentAt}, claimed_at = null, "
            + "next_retry_at = null, last_error = null "
            + "where id = #{id} and status = 'SENDING' and deleted = 0")
    int markSent(@Param("id") Long id, @Param("sentAt") LocalDateTime sentAt);

    @Update("update mail_delivery set status = #{status}, attempts = #{attempts}, claimed_at = null, "
            + "next_retry_at = #{nextRetryAt}, last_error = #{lastError} "
            + "where id = #{id} and status = 'SENDING' and deleted = 0")
    int recordFailure(@Param("id") Long id, @Param("status") String status,
                      @Param("attempts") Integer attempts,
                      @Param("nextRetryAt") LocalDateTime nextRetryAt,
                      @Param("lastError") String lastError);

    @Update("update mail_delivery set recipient = #{recipient}, status = 'PENDING', attempts = 0, "
            + "claimed_at = null, next_retry_at = null, last_error = null "
            + "where id = #{id} and status = 'DEAD' and deleted = 0")
    int resetForManualRetry(@Param("id") Long id, @Param("recipient") String recipient);
}
