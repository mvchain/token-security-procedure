package com.mvc.security.procedure.service;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageInfo;
import com.mvc.security.procedure.bean.*;
import com.mvc.security.procedure.bean.dto.BtcOutput;
import com.mvc.security.procedure.bean.dto.NewAccountDTO;
import com.mvc.security.procedure.dao.AccountMapper;
import com.mvc.security.procedure.dao.MissionMapper;
import com.mvc.security.procedure.dao.OrderMapper;
import com.mvc.security.procedure.util.ObjectUtil;
import com.neemre.btcdcli4j.core.BitcoindException;
import com.neemre.btcdcli4j.core.CommunicationException;
import com.neemre.btcdcli4j.core.client.BtcdClient;
import com.neemre.btcdcli4j.core.domain.Output;
import com.neemre.btcdcli4j.core.domain.OutputOverview;
import com.neemre.btcdcli4j.core.domain.SignatureResult;
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
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.*;
import org.web3j.utils.Numeric;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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
@Transactional(rollbackFor = RuntimeException.class)
public class OrderService {

    @Autowired
    OrderMapper orderMapper;
    @Autowired
    AccountMapper accountMapper;
    @Autowired
    MissionMapper missionMapper;
    @Autowired
    private BtcdClient btcdClient;
    @Value("${usdt.propId}")
    private Integer propId;

    protected static ObjectMapper objectMapper = new ObjectMapper();


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
            if (order.getValue() == null) {
                order.setValue(BigDecimal.ZERO);
            }
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
        List<Map<String, Object>> result = new ArrayList<>(ordersList.size());
        ordersList.stream().forEach(obj -> {
            BlockSign sign = new BlockSign();
            sign.setContractAddress(obj.getContractAddress());
            sign.setFromAddress(obj.getFromAddress());
            sign.setOprType(obj.getOprType());
            sign.setOrderId(obj.getOrderId());
            sign.setSign(obj.getSignature());
            sign.setStartedAt(0L);
            sign.setStatus(0);
            sign.setToAddress(obj.getToAddress());
            sign.setTokenType(obj.getTokenType());
            try {
                Map<String, Object> map = ObjectUtil.convertBean(sign);
                if (obj.getStatus() != 9) {
                    result.add(map);
                }
            } catch (IntrospectionException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        });
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

    private String getPvKey(String address) {
        Account account = new Account();
        account.setAddress(address);
        account = accountMapper.selectOne(account);
        return null == account ? null : account.getPrivateKey();
    }

    public void updateErc20OrdersSig(Orders order, Mission mission) throws Exception {
        String pvKey = getPvKey(order.getFromAddress());
        if (null == pvKey) {
            order.setSignature("");
            orderMapper.updateByPrimaryKey(order);
            mission.setComplete(mission.getComplete() + 1);
            updateMission(mission);
            return;
        }
        Function function = null;
        Uint256 limit = new Uint256(new BigInteger("10000000000000").multiply(BigInteger.TEN.pow(18)));
        if (order.getOprType().equals(2)) {
            function = new Function(
                    "approve",
                    Arrays.asList(new Address(order.getFeeAddress()), limit),
                    Collections.singletonList(new TypeReference<Bool>() {
                    }));
        } else if (order.getOprType().equals(1)) {
            function = new Function(
                    "transfer",
                    Arrays.asList(new Address(order.getToAddress()), new Uint256(order.getValue().toBigInteger())),
                    Collections.singletonList(new TypeReference<Bool>() {
                    }));
            String encodedFunction = FunctionEncoder.encode(function);
        } else if (order.getOprType().equals(0)) {
            function = new Function(
                    "transferFrom",
                    Arrays.asList(new Address(order.getToAddress()), new Address(order.getFromAddress()), new Uint256(order.getValue().toBigInteger())),
                    Collections.singletonList(new TypeReference<Bool>() {
                    }));
        }
        String encodedFunction = FunctionEncoder.encode(function);
        RawTransaction rawTransaction = RawTransaction.createTransaction(
                order.getNonce(),
                order.getGasPrice().toBigInteger(),
                order.getGasLimit().toBigInteger(),
                order.getContractAddress(),
                encodedFunction);
        ECKeyPair ecKeyPair = ECKeyPair.create(new BigInteger(pvKey));
        Credentials ALICE = Credentials.create(ecKeyPair);
        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, ALICE);
        String hexValue = Numeric.toHexString(signedMessage);
        order.setSignature(hexValue);
        orderMapper.updateByPrimaryKey(order);
        mission.setComplete(mission.getComplete() + 1);
        updateMission(mission);
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
        } else if (tokenType.equalsIgnoreCase("BTC") || tokenType.equalsIgnoreCase("USDT")) {
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

    public void updateEthOrderSig(Orders order, Mission mission) {
        Account account = new Account();
        account.setAddress(order.getFromAddress());
        account = accountMapper.selectOne(account);
        ECKeyPair ecKeyPair = ECKeyPair.create(new BigInteger(account.getPrivateKey()));
        Credentials ALICE = Credentials.create(ecKeyPair);
        BigInteger nonce = order.getNonce();
        BigInteger value = order.getValue().toBigInteger();
        if (null == order.getOrderId()) {
            //汇总才没有orderid, 汇总时需要扣除手续费
            value = value.subtract(order.getGasPrice().toBigInteger());
        }
        RawTransaction transaction = RawTransaction.createEtherTransaction(nonce, order.getGasPrice().toBigInteger(), gethLimit, order.getToAddress(), value);
        byte[] signedMessage = TransactionEncoder.signMessage(transaction, ALICE);
        String hexValue = Numeric.toHexString(signedMessage);
        order.setSignature(hexValue);
        orderMapper.updateByPrimaryKey(order);
        mission.setComplete(mission.getComplete() + 1);
        updateMission(mission);
    }

    public void updateBtcOrdersSig(Orders order, Mission mission) throws BitcoindException, CommunicationException, IOException {
        Account account = new Account();
        account.setAddress(order.getFromAddress());
        account = accountMapper.selectOne(account);
        try {
            //每次都重新导入,防止钱包更换后需要手动重新导入数据库内容
            btcdClient.importPrivKey(account.getPrivateKey(), account.getAddress());
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<BtcOutput> btcOutputs = JSON.parseArray(order.getContractAddress(), BtcOutput.class);
        btcOutputs = btcOutputs.stream().filter(obj -> obj.getAmount().compareTo(order.getGasPrice()) > 0).collect(Collectors.toList());
        if (btcOutputs.size() <= 0) {
            order.setStatus(9);
            orderMapper.updateByPrimaryKey(order);
            mission.setComplete(mission.getComplete() + 1);
            updateMission(mission);
            return;
        }
        List<Output> unspent = getOutput(btcOutputs);

        List<OutputOverview> unspentView = getOutputView(unspent);
        //Send how much tether.
        String simpleSendResult = objectMapper.readValue(btcdClient.remoteCall("omni_createpayload_simplesend", Arrays.asList(propId, String.valueOf(order.getValue()))).toString(), String.class);
        //Create BTC Raw Transaction.
        String createRawTransactionResult = btcdClient.createRawTransaction(unspentView, new HashMap<>());
        //Add omni token(Tehter) data to the transaction.
        String combinedResult = objectMapper.readValue(btcdClient.remoteCall("omni_createrawtx_opreturn",
                Arrays.asList(createRawTransactionResult.toString(), simpleSendResult)).toString(),
                String.class);
        //.Add collect/to address.
        String referenceResult = objectMapper.readValue(btcdClient.remoteCall("omni_createrawtx_reference",
                Arrays.asList(combinedResult, order.getToAddress())).toString(),
                String.class);
        //Add fee.
        List<OmniCreaterawtxChangeRequiredEntity> entitys = getBtcEntitys(unspent);

        String changeResult = objectMapper.readValue(btcdClient.remoteCall("omni_createrawtx_change",
                Arrays.asList(referenceResult, entitys, order.getFromAddress(), order.getGasPrice())).toString(),
                String.class);
        SignatureResult hex = btcdClient.signRawTransaction(changeResult, unspent);
        if (hex.getComplete()) {
            order.setSignature(hex.getHex());
        } else {
            order.setStatus(9);
        }
        orderMapper.updateByPrimaryKey(order);
        mission.setComplete(mission.getComplete() + 1);
        updateMission(mission);
    }

    private List<OutputOverview> getOutputView(List<Output> unspents) {
        List<OutputOverview> list = new ArrayList<>(unspents.size());
        unspents.forEach(unspent -> {
            OutputOverview overview = new OutputOverview();
            overview.setVOut(unspent.getVOut());
            overview.setTxId(unspent.getTxId());
            list.add(unspent);
        });
        return list;
    }

    private List<OmniCreaterawtxChangeRequiredEntity> getBtcEntitys(List<Output> unspents) {
        List<OmniCreaterawtxChangeRequiredEntity> list = new ArrayList<>();
        unspents.forEach(unspent -> {
            OmniCreaterawtxChangeRequiredEntity entity = new OmniCreaterawtxChangeRequiredEntity(unspent.getTxId(), unspent.getVOut(), unspent.getScriptPubKey(), unspent.getAmount());
            list.add(entity);
        });
        return list;
    }

    private List<Output> getOutput(List<BtcOutput> btcOutputs) {
        List<Output> list = new ArrayList<>();
        btcOutputs.forEach(btcOutput -> {
            Output output = new Output();
            output.setAccount(btcOutput.getAccount());
            output.setAddress(btcOutput.getAddress());
            output.setAmount(btcOutput.getAmount());
            output.setConfirmations(btcOutput.getConfirmations());
            output.setRedeemScript(btcOutput.getRedeemScript());
            output.setScriptPubKey(btcOutput.getScriptPubKey());
            output.setSpendable(true);
            output.setTxId(btcOutput.getTxId());
            output.setVOut(btcOutput.getVOut());
            list.add(output);
        });
        return list;
    }
//
//    public void updateBtcOrdersSig(Orders order, Mission mission) throws BitcoindException, CommunicationException {
//        String listUnspentStr = order.getToAddress();
//        List<Output> list = JSON.parseArray(listUnspentStr, Output.class);
//        SignatureResult res = btcdClient.signRawTransactionWithWallet(order.getFromAddress(), list);
//        if (!res.getComplete()) {
//            System.out.println("签名失败");
//            return;
//        }
//        order.setSignature(res.getHex());
//        orderMapper.updateByPrimaryKey(order);
//        mission.setComplete(mission.getComplete() + 1);
//        updateMission(mission);
//    }
}
