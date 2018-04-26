package com.mvc.security.procedure.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageInfo;
import com.mvc.security.procedure.bean.Account;
import com.mvc.security.procedure.bean.Mission;
import com.mvc.security.procedure.bean.Orders;
import com.mvc.security.procedure.bean.dto.NewAccountDTO;
import com.mvc.security.procedure.dao.AccountMapper;
import com.mvc.security.procedure.dao.MissionMapper;
import com.mvc.security.procedure.dao.OrderMapper;
import com.mvc.security.procedure.util.TextSearchFile;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import org.web3j.protocol.Web3j;
import org.web3j.protocol.admin.Admin;
import org.web3j.protocol.admin.methods.response.NewAccountIdentifier;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static org.web3j.tx.Contract.GAS_LIMIT;
import static org.web3j.tx.ManagedTransaction.GAS_PRICE;

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
    Web3j web3j;
    @Autowired
    Admin admin;
    @Value("${keyFile.path}")
    String folder;
    ObjectMapper objectMapper = new ObjectMapper();

    public Account getAdmin(Integer type) throws IOException, CipherException {
        Account account = new Account();
        account.setType(type);
        account.setIsAdmin(1);
        account = accountMapper.selectOne(account);
        if (null == account) {
            account = newAccount(type, 1, BigInteger.ZERO);
        }
        return account;
    }

    private Account newAccount(Integer type, Integer isAdmin, BigInteger missionId) throws IOException, CipherException {
        Account account = new Account();
        String pass = UUID.randomUUID().toString();
        account.setIsAdmin(isAdmin);
        account.setType(type);
        account.setPass(pass);
        NewAccountIdentifier result = admin.personalNewAccount(pass).send();
        account.setAddress(result.getAccountId());
        File[] fileArr = TextSearchFile.searchFile(new File(folder), result.getAccountId().substring(2));
        Assert.isTrue(fileArr.length == 1, "newAccount Error");
        File keyStore = fileArr[0];
        String source = FileUtils.readFileToString(keyStore);
        WalletFile file = objectMapper.readValue(source, WalletFile.class);
        ECKeyPair ecKeyPair = Wallet.decrypt(pass, file);
        account.setPrivateKey(String.valueOf(ecKeyPair.getPrivateKey()));
        account.setMissionId(missionId);
        accountMapper.insert(account);
        return account;
    }

    public PageInfo<Mission> getMission(Integer type) {
        Mission mission = new Mission();
        mission.setType(type);
        return new PageInfo<>(missionMapper.select(mission));
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
            return map;
        }).collect(Collectors.toList());
        return result;
    }

    public void importOrders(List<Orders> list) {
        Integer total = 0;
        for (Orders order : list) {
            Orders temp = new Orders();
            temp.setOrderId(order.getOrderId());
            temp = orderMapper.selectOne(temp);
            if (null == temp) {
                order.setId(null);
                total++;
            }
        }
        Mission mission = new Mission();
        mission.setTotal(total);
        mission.setComplete(0);
        mission.setType(2);
        missionMapper.insert(mission);
        for (Orders order : list) {
            Orders temp = new Orders();
            temp.setOrderId(order.getOrderId());
            temp = orderMapper.selectOne(temp);
            if (null == temp) {
                order.setId(null);
                order.setMissionId(mission.getId());
                orderMapper.insert(order);
            }
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
        RawTransaction transaction = RawTransaction.createEtherTransaction(nonce, GAS_PRICE.divide(BigInteger.valueOf(10)), GAS_LIMIT, order.getToAddress(), Convert.toWei(order.getValue(), Convert.Unit.ETHER).toBigInteger());
        byte[] signedMessage = TransactionEncoder.signMessage(transaction, ALICE);
        String hexValue = Numeric.toHexString(signedMessage);
        order.setSignature(hexValue);
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
        Function function = new Function("transfer", Arrays.<Type>asList(new Address(order.getToAddress()), value), Collections.singletonList(new TypeReference<Bool>() {}));
        String data = FunctionEncoder.encode(function);
        RawTransaction transaction = RawTransaction.createTransaction(nonce, GAS_PRICE.divide(BigInteger.valueOf(10)), GAS_LIMIT, tokenConfig.get("address"), data);
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
        return missionMapper.accountMission(mission);
    }

    public void newAccount(String tokenType, Mission mission) throws IOException, CipherException {
        if (tokenType.equalsIgnoreCase("ETH")) {
            newAccount(1, 0, mission.getId());
            mission.setComplete(mission.getComplete() + 1);
            updateMission(mission);
        }
    }
}
