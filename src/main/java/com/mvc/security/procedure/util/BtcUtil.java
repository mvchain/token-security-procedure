package com.mvc.security.procedure.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neemre.btcdcli4j.core.BitcoindException;
import com.neemre.btcdcli4j.core.CommunicationException;
import com.neemre.btcdcli4j.core.client.BtcdClient;
import com.neemre.btcdcli4j.core.domain.Output;
import com.neemre.btcdcli4j.core.domain.OutputOverview;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author qiyichen
 * @create 2019/1/25 11:58
 */
public class BtcUtil {

    protected static ObjectMapper objectMapper = new ObjectMapper();

    public static BigDecimal getBtcBalance(BtcdClient btcdClient, String address) throws BitcoindException, CommunicationException {
        List<Output> unspent = btcdClient.listUnspent(0, Integer.MAX_VALUE, Arrays.asList(address));
        return getBtcBalance(unspent);
    }

    public static BigDecimal getBtcBalance(List<Output> unspent) {
        return unspent.stream().map(obj -> obj.getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static List<OutputOverview> transOutputs(List<Output> unspent) {
        List<OutputOverview> input = new ArrayList<>(unspent.size());
        for (Output obj : unspent) {
            //使用后余额也还原到该地址
            input.add(obj);
        }
        return input;
    }

}
