package com.mvc.security.procedure.dao;

import com.mvc.security.procedure.bean.Mission;
import com.mvc.security.procedure.bean.TotalBalance;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

import java.math.BigInteger;
import java.util.List;

/**
 * @author ethands
 */
public interface MissionMapper extends Mapper<Mission> {
    @Select("select * from mission where type = #{type} and token_type = #{tokenType} and total != complete limit 1")
    Mission accountMission(Mission mission);

    @Select("SELECT sum(`value`) total, token_type FROM orders WHERE mission_id = #{id} GROUP BY token_type")
    List<TotalBalance> totalBalance(BigInteger id);
}
