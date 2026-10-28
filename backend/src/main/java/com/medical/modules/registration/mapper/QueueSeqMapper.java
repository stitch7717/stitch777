package com.medical.modules.registration.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

/**
 * 排队号计数器：按「科室 + 日期」原子自增。
 */
public interface QueueSeqMapper {

    @Insert("""
            INSERT INTO biz_queue_seq (dept_id, seq_date, current_no)
            VALUES (#{deptId}, #{seqDate}, 1)
            ON DUPLICATE KEY UPDATE current_no = current_no + 1
            """)
    int bump(@Param("deptId") Long deptId, @Param("seqDate") LocalDate seqDate);

    @Select("""
            SELECT current_no FROM biz_queue_seq
            WHERE dept_id = #{deptId} AND seq_date = #{seqDate}
            """)
    Integer current(@Param("deptId") Long deptId, @Param("seqDate") LocalDate seqDate);
}
