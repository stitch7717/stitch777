package com.medical.modules.registration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medical.modules.registration.entity.BizRegistration;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface RegistrationMapper extends BaseMapper<BizRegistration> {

    /**
     * 事务内锁定就诊行：收费/退费等跨表状态机以此串行化同一就诊的并发操作。
     */
    @Select("SELECT * FROM biz_registration WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    BizRegistration selectByIdForUpdate(@Param("id") Long id);
}
