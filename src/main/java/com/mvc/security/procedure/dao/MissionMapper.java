package com.mvc.security.procedure.dao;

import com.mvc.security.procedure.bean.Mission;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

/**
 * @author ethands
 */
public interface MissionMapper extends Mapper<Mission> {
    @Select("select * from mission where type = #{type} and total != complete limit 1")
    Mission accountMission(Mission mission);
}
