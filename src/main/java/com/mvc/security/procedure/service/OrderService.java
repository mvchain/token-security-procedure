package com.mvc.security.procedure.service;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageInfo;
import com.mvc.security.procedure.bean.Account;
import com.mvc.security.procedure.bean.Gas;
import com.mvc.security.procedure.bean.Mission;
import com.mvc.security.procedure.bean.Orders;
import com.mvc.security.procedure.bean.dto.NewAccountDTO;
import com.mvc.security.procedure.dao.AccountMapper;
import com.mvc.security.procedure.dao.MissionMapper;
import com.mvc.security.procedure.dao.OrderMapper;
import com.neemre.btcdcli4j.core.BitcoindException;
import com.neemre.btcdcli4j.core.CommunicationException;
import com.neemre.btcdcli4j.core.client.BtcdClient;
import com.neemre.btcdcli4j.core.domain.Output;
import com.neemre.btcdcli4j.core.domain.SignatureResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.*;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

/**
 * order service
 *
 * @author qiyichen
 * @create 2018/4/18 15:20
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class OrderService {

    @Autowired
    OrderMapper orderMapper;
    @Autowired
    AccountMapper accountMapper;
    @Autowired
    MissionMapper missionMapper;
    @Autowired
    private BtcdClient btcdClient;


    static BigInteger gethLimit = BigInteger.valueOf(21000);

    public Account getAdmin(Integer type) throws Exception {

        Account account = new Account();
        account.setType(type);
        account.setIsAdmin(1);
        account = accountMapper.selectOne(account);
        if (null == account) {
            account = newAccount(type, 1, BigInteger.ZERO);
        }
        return account;
    }

    private Account btcNewAccount(int type, int isAdmin, BigInteger missionId) throws BitcoindException, CommunicationException {
        Account account = new Account();
        account.setIsAdmin(isAdmin);
        account.setType(type);
        String address = btcdClient.getNewAddress();
        String pvKey = btcdClient.dumpPrivKey(address);
        account.setPrivateKey(pvKey);
        account.setMissionId(missionId);
        account.setAddress(address);
        accountMapper.insert(account);
        return account;
    }

    private Account newAccount(Integer type, Integer isAdmin, BigInteger missionId) throws Exception {
        Account account = new Account();
        account.setIsAdmin(isAdmin);
        account.setType(type);
        ECKeyPair ecKeyPair = Keys.createEcKeyPair();
        account.setPrivateKey(String.valueOf(ecKeyPair.getPrivateKey()));
        account.setMissionId(missionId);
        account.setAddress(Credentials.create(ecKeyPair).getAddress());
        accountMapper.insert(account);
        return account;
    }

    public PageInfo<Mission> getMission(Integer type) {
        Mission mission = new Mission();
        mission.setType(type);
        List<Mission> result = missionMapper.select(mission);
        if (2 == type) {
            result.stream().forEach(obj -> obj.setTotalBalance(
                    missionMapper.totalBalance(obj.getId())
            ));
        }
        return new PageInfo<>(result);
    }

    public void newAccounts(NewAccountDTO newAccountDTO) {
        Mission mission = new Mission();
        mission.setType(1);
        mission.setComplete(0);
        mission.setTokenType(newAccountDTO.getTokenType());
        mission.setTotal(newAccountDTO.getNumber());
        missionMapper.insert(mission);
    }

    public List<Map<String, Object>> getAccountList(BigInteger id) {
        Mission mission = new Mission();
        mission.setId(id);
        mission = missionMapper.selectByPrimaryKey(mission);
        Assert.notNull(mission, "记录不存在");
        Assert.isTrue(mission.getComplete().equals(mission.getTotal()), "创建未完成");
        Account account = new Account();
        account.setMissionId(id);
        List<Account> accounts = accountMapper.select(account);
        List<Map<String, Object>> result = accounts.stream().map(ac -> {
            Map<String, Object> map = new HashMap<>();
            map.put("address", ac.getAddress());
            map.put("tokenType", ac.getType() == 1 ? "ETH" : "BTC");
            return map;
        }).collect(Collectors.toList());
        return result;
    }

    public void importOrders(List<Orders> list) {
        if (list.size() == 0) {
            return;
        }
        Mission mission = new Mission();
        mission.setTotal(list.size());
        mission.setComplete(0);
        mission.setType(2);
        missionMapper.insert(mission);
        for (Orders order : list) {
            order.setMissionId(mission.getId());
            orderMapper.insert(order);
        }
    }

    public List<Map<String, Object>> getSign(BigInteger id) {
        Mission mission = new Mission();
        mission.setId(id);
        mission = missionMapper.selectByPrimaryKey(mission);
        Assert.notNull(mission, "记录不存在");
        Assert.isTrue(mission.getComplete().equals(mission.getTotal()), "创建未完成");
        Orders orders = new Orders();
        orders.setMissionId(id);
        List<Orders> ordersList = orderMapper.select(orders);
        List<Map<String, Object>> result = ordersList.stream().map(ac -> {
            Map<String, Object> map = new HashMap<>();
            map.put("orderId", ac.getOrderId());
            map.put("type", ac.getTokenType());
            map.put("signature", ac.getSignature());
            map.put("value", ac.getValue());
            map.put("fee", ac.getFee());
            return map;
        }).collect(Collectors.toList());
        return result;
    }

    public Mission accountMission() {
        Mission mission = new Mission();
        mission.setType(1);
        return missionMapper.accountMission(mission);
    }

    public List<Orders> getOrders(BigInteger id) {
        Orders orders = new Orders();
        orders.setMissionId(id);
        return orderMapper.select(orders);
    }

    public void updateMission(Mission mission) {
        missionMapper.updateByPrimaryKey(mission);
    }

    public void updateEthOrdersSig(Orders order, Mission mission) throws Exception {
        Account account = new Account();
        account.setAddress(order.getFromAddress());
        account = accountMapper.selectOne(account);
        ECKeyPair ecKeyPair = ECKeyPair.create(new BigInteger(account.getPrivateKey()));
        Credentials ALICE = Credentials.create(ecKeyPair);
        BigInteger nonce = getNonce(order);
        BigDecimal value = order.getValue();
        if (null == order.getOrderId()) {
            //汇总才没有orderid, 汇总时需要扣除手续费
            value = value.subtract(Convert.fromWei(new BigDecimal(gethLimit.multiply(order.getFee().toBigInteger())), Convert.Unit.ETHER));
        }
        RawTransaction transaction = RawTransaction.createEtherTransaction(nonce, order.getFee().toBigInteger(), gethLimit, order.getToAddress(), Convert.toWei(value, Convert.Unit.ETHER).toBigInteger());
        byte[] signedMessage = TransactionEncoder.signMessage(transaction, ALICE);
        String hexValue = Numeric.toHexString(signedMessage);
        order.setSignature(hexValue);
        order.setFee(Convert.fromWei(new BigDecimal(gethLimit.multiply(order.getFee().toBigInteger())), Convert.Unit.ETHER));
        orderMapper.updateByPrimaryKey(order);
        mission.setComplete(mission.getComplete() + 1);
        updateMission(mission);
    }

    public void updateErc20OrderSig(Orders order, Mission mission, Map<String, String> tokenConfig) throws Exception {
        Account account = new Account();
        account.setAddress(order.getFromAddress());
        account = accountMapper.selectOne(account);
        ECKeyPair ecKeyPair = ECKeyPair.create(new BigInteger(account.getPrivateKey()));
        Credentials ALICE = Credentials.create(ecKeyPair);
        BigInteger nonce = getNonce(order);
        // Transfer value to default format (without decimals), generally the Decimals is 18.
        Uint256 value = new Uint256(order.getValue().multiply(new BigDecimal(Math.pow(10, Integer.parseInt(tokenConfig.get("decimals"))))).toBigInteger());
        Function function = new Function("transfer", Arrays.<Type>asList(new Address(order.getToAddress()), value), Collections.singletonList(new TypeReference<Bool>() {
        }));
        String data = FunctionEncoder.encode(function);
        RawTransaction transaction = RawTransaction.createTransaction(nonce, order.getFee().toBigInteger(), gethLimit, tokenConfig.get("address"), data);
        byte[] signedMessage = TransactionEncoder.signMessage(transaction, ALICE);
        String hexValue = Numeric.toHexString(signedMessage);
        order.setSignature(hexValue);
        orderMapper.updateByPrimaryKey(order);
        mission.setComplete(mission.getComplete() + 1);
        updateMission(mission);
    }

//    static BigInteger getNonce(Web3j web3j, String address) throws Exception {
//        EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
//                address, DefaultBlockParameterName.LATEST).sendAsync().get();
//        return ethGetTransactionCount.getTransactionCount();
//    }

    static BigInteger getNonce(Orders order) throws Exception {
        return order.getNonce();
    }

    public Mission signMission() {
        Mission mission = new Mission();
        mission.setType(2);
        mission.setTokenType("ETH");
        return missionMapper.accountMission(mission);
    }

    public void newAccount(String tokenType, Mission mission) throws Exception {
        if (tokenType.equalsIgnoreCase("ETH")) {
            newAccount(1, 0, mission.getId());
            mission.setComplete(mission.getComplete() + 1);
            updateMission(mission);
        } else if (tokenType.equalsIgnoreCase("BTC")) {
            btcNewAccount(2, 0, mission.getId());
            mission.setComplete(mission.getComplete() + 1);
            updateMission(mission);
        }
    }

    public static Gas setGas(Gas gas) {
        gethLimit = gas.getGasLimit();
        return getGas();
    }

    public static Gas getGas() {
        Gas gas = new Gas();
        gas.setGasLimit(gethLimit);
        return gas;
    }

    public void delAccount(BigInteger id) {
        missionMapper.deleteByPrimaryKey(id);
    }

    public void updateBtcOrdersSig(Orders order, Mission mission) throws BitcoindException, CommunicationException {
        String listUnspentStr = order.getToAddress();
        List<Output> list = JSON.parseArray(listUnspentStr, Output.class);
        SignatureResult res = btcdClient.signRawTransactionWithWallet(order.getFromAddress(), list);
        if (!res.getComplete()) {
            System.out.println("签名失败");
            return;
        }
        order.setSignature(res.getHex());
        orderMapper.updateByPrimaryKey(order);
        mission.setComplete(mission.getComplete() + 1);
        updateMission(mission);
    }
}
