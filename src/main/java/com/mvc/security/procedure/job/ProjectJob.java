package com.mvc.security.procedure.job;

import com.mvc.security.procedure.bean.Mission;
import com.mvc.security.procedure.bean.Orders;
import com.mvc.security.procedure.service.OrderService;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * eth job
 *
 * @author qiyichen
 * @create 2018/3/16 14:20
 */
@Component
@Log4j
public class ProjectJob {

    static ConcurrentHashMap<BigInteger, Boolean> jobMap = new ConcurrentHashMap<>();

    @Autowired
    OrderService orderService;

    @Scheduled(cron = "*/5 * * * * ?")
    public void newAccount() throws Exception {
        Mission mission = orderService.accountMission();
        if (null != mission && jobMap.get(mission.getId()) == null) {
            jobMap.put(mission.getId(), true);
            for (int i = mission.getComplete(); i < mission.getTotal(); i++) {
                orderService.newAccount(mission.getTokenType(), mission);
            }
        }
    }

    @Scheduled(cron = "*/5 * * * * ?")
    public void sign() throws Exception {
        Mission mission = orderService.signMission();
        if (null != mission && jobMap.get(mission.getId()) == null) {
            jobMap.put(mission.getId(), true);
            List<Orders> orders = orderService.getOrders(mission.getId());
            for (Orders order : orders) {
                orderService.updateOrdersSig(order, mission);
            }
        }
    }


}
