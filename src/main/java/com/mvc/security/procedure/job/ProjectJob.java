package com.mvc.security.procedure.job;

import com.mvc.security.procedure.bean.Mission;
import com.mvc.security.procedure.bean.Orders;
import com.mvc.security.procedure.config.TokenConfig;
import com.mvc.security.procedure.service.OrderService;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.web3j.crypto.CipherException;

import java.io.IOException;
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

    @Autowired
    TokenConfig tokenConfig;

    @Scheduled(cron = "*/5 * * * * ?")
    public void newAccount() {
        Mission mission = orderService.accountMission();
        if (null != mission && jobMap.get(mission.getId()) == null) {
            jobMap.put(mission.getId(), true);
            for (int i = mission.getComplete(); i < mission.getTotal(); i++) {
                try {
                    orderService.newAccount(mission.getTokenType(), mission);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            jobMap.remove(mission.getId());
        }
    }

    @Scheduled(cron = "*/5 * * * * ?")
    public void sign() {
        Mission mission = orderService.signMission();
        if (null != mission && jobMap.get(mission.getId()) == null) {
            jobMap.put(mission.getId(), true);
            List<Orders> orders = orderService.getOrders(mission.getId());
            for (Orders order : orders) {
                try {
                    if ("ETH".equalsIgnoreCase(order.getTokenType())) {
                        orderService.updateEthOrdersSig(order, mission);
                    } else if (tokenConfig.getErc20().keySet().contains(order.getTokenType().toLowerCase())) {
                        orderService.updateErc20OrderSig(order, mission, tokenConfig.getErc20().get(order.getTokenType().toLowerCase()));
                    } else {
                        throw new Exception("Token symbol not recognized.");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            jobMap.remove(mission.getId());
        }
    }


}
