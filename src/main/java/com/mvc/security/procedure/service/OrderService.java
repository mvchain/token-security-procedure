package com.mvc.security.procedure.service;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageInfo;
import com.mvc.security.procedure.bean.Account;
import com.mvc.security.procedure.bean.*;
import com.mvc.security.procedure.bean.dto.BtcOutput;
import com.mvc.security.procedure.bean.dto.NewAccountDTO;
import com.mvc.security.procedure.dao.AccountMapper;
import com.mvc.security.procedure.dao.MissionMapper;
import com.mvc.security.procedure.dao.OrderMapper;
import com.mvc.security.procedure.util.BtcUtil;
import com.mvc.security.procedure.util.ObjectUtil;
import com.neemre.btcdcli4j.core.BitcoindException;
import com.neemre.btcdcli4j.core.CommunicationException;
import com.neemre.btcdcli4j.core.client.BtcdClient;
import com.neemre.btcdcli4j.core.domain.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
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
import org.web3j.crypto.RawTransaction;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.beans.IntrospectionException;
import java.io.File;
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
                if (StringUtils.isNotBlank(obj.getSignature())) {
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
        missionMapper.updateByPrimaryKeySelective(mission);
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
            orderMapper.updateByPrimaryKeySelective(order);
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
        orderMapper.updateByPrimaryKeySelective(order);
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
        RawTransaction transaction = RawTransaction.createEtherTransaction(nonce, order.getGasPrice().toBigInteger(), gethLimit, order.getToAddress(), Convert.toWei(order.getValue(), Convert.Unit.ETHER).toBigInteger());
        byte[] signedMessage = TransactionEncoder.signMessage(transaction, ALICE);
        String hexValue = Numeric.toHexString(signedMessage);
        order.setSignature(hexValue);
        orderMapper.updateByPrimaryKeySelective(order);
        mission.setComplete(mission.getComplete() + 1);
        updateMission(mission);
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

    private String getBtcSign(Orders order, List<Output> unspent) throws BitcoindException, CommunicationException {
        BigDecimal balance = BtcUtil.getBtcBalance(unspent);
        Map<String, BigDecimal> outMap = new HashMap<>(2);
        outMap.put(order.getToAddress(), order.getValue());
        BigDecimal collectValue = balance.subtract(order.getGasPrice()).subtract(order.getValue());
        outMap.put(order.getFromAddress(), collectValue);
        String raw = btcdClient.createRawTransaction(BtcUtil.transOutputs(unspent), outMap);
        SignatureResult signResult = btcdClient.signRawTransaction(raw, unspent);
        if (signResult.getComplete()) {
            return signResult.getHex();
        } else {
            return null;
        }
    }

    private void updateMission(Boolean result, String hex, Mission mission, Orders order) {
        if (result) {
            order.setStatus(0);
            order.setSignature(hex);
        } else {
            order.setStatus(9);
        }
        orderMapper.updateByPrimaryKeySelective(order);
        mission.setComplete(mission.getComplete() + 1);
        updateMission(mission);
    }

    public void updateBtcOrdersSig(Mission mission, List<Orders> btcList) throws Exception {
        List<Output> unspent = btcList.size() == 0 ? null : JSON.parseArray(btcList.get(0).getContractAddress(), Output.class);
        for (Orders order : btcList) {
            importAccount(order);
            String hex = null;
            String scriptPubKey = getPubKey(unspent, order);
            BigDecimal balance = BtcUtil.getBtcBalance(unspent);
            Boolean isBtc = StringUtils.isBlank(order.getFeeAddress());
            if (isBtc) {
                //btc
                hex = getBtcSign(order, unspent);
            } else {
                //usdt
                hex = getUsdtSign(unspent, order.getFromAddress(), order.getToAddress(), order.getValue(), order.getGasPrice());
            }
            if (null == hex) {
                order.setStatus(9);
            } else {
                order.setSignature(hex);

                BigDecimal collectValue = isBtc ? balance.subtract(order.getGasPrice()).subtract(order.getValue()) : balance.subtract(order.getGasPrice());
                String hash = btcdClient.decodeRawTransaction(hex).getTxId();
                unspent = new ArrayList<>();
                Output output = new Output();
                output.setVOut(isBtc ? 1 : 0);
                output.setTxId(hash);
                output.setAddress(order.getFromAddress());
                output.setScriptPubKey(scriptPubKey);
                output.setAmount(collectValue);
                unspent.add(output);
            }
            orderMapper.updateByPrimaryKeySelective(order);
            mission.setComplete(mission.getComplete() + 1);
            updateMission(mission);
        }

    }

    private void importAccount(Orders order) {
        Account account = new Account();
        account.setAddress(order.getFromAddress());
        account = accountMapper.selectOne(account);
        try {
            //每次都重新导入,防止钱包更换后需要手动重新导入数据库内容
            btcdClient.importPrivKey(account.getPrivateKey(), account.getAddress());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getUsdtSign(List<Output> unspent, String from, String to, BigDecimal value, BigDecimal fee) throws BitcoindException, CommunicationException, IOException {
        List<OutputOverview> unspentView = BtcUtil.transOutputs(unspent);
        //Send how much tether.
        String simpleSendResult = objectMapper.readValue(btcdClient.remoteCall("omni_createpayload_simplesend", Arrays.asList(propId, String.valueOf(value))).toString(), String.class);
        //Create BTC Raw Transaction.
        String createRawTransactionResult = btcdClient.createRawTransaction(unspentView, new HashMap<>());
        //Add omni token(Tehter) data to the transaction.
        String combinedResult = objectMapper.readValue(btcdClient.remoteCall("omni_createrawtx_opreturn",
                Arrays.asList(createRawTransactionResult.toString(), simpleSendResult)).toString(),
                String.class);
        //.Add collect/to address.
        String referenceResult = objectMapper.readValue(btcdClient.remoteCall("omni_createrawtx_reference",
                Arrays.asList(combinedResult, to)).toString(),
                String.class);
        //Add fee.
        List<OmniCreaterawtxChangeRequiredEntity> entitys = getBtcEntitys(unspent);
        String changeResult = objectMapper.readValue(btcdClient.remoteCall("omni_createrawtx_change",
                Arrays.asList(referenceResult, entitys, from, fee)).toString(),
                String.class);
        SignatureResult hex = btcdClient.signRawTransaction(changeResult, unspent);
        if (hex.getComplete()) {
            return hex.getHex();
        } else {
            return null;
        }
    }


    private String getPubKey(List<Output> unspent, Orders order) {
        String scriptPubKey = "";
        for (Output obj : unspent) {
            if (obj.getAddress().equals(order.getFromAddress())) {
                scriptPubKey = obj.getScriptPubKey();
            }
        }
        return scriptPubKey;
    }

    private Boolean isAccount(String address) {
        Account account = new Account();
        account.setAddress(address);
        account = accountMapper.selectOne(account);
        return null != account;
    }

    public void updateBtcCollectOrdersSig(Mission mission, List<Orders> btcList) throws BitcoindException, IOException, CommunicationException {
        //collect btc
        List<Orders> btcOrders = btcList.stream().filter(obj -> obj.getNonce().equals(BigInteger.ZERO)).collect(Collectors.toList());
        List<Output> btcUnspent = JSON.parseArray(btcList.get(0).getContractAddress(), Output.class);
        btcUnspent = btcUnspent.stream().filter(obj -> obj.getAmount().compareTo(btcOrders.get(0).getGasPrice()) > 0 && isAccount(obj.getAddress())).collect(Collectors.toList());
        List<Output> btcOutputs = new ArrayList<>();
        BigDecimal balance = BtcUtil.getBtcBalance(btcUnspent);
        BigDecimal collectSum = balance.subtract(btcOrders.get(0).getGasPrice().multiply(BigDecimal.valueOf(btcUnspent.size() / 10 + 1)));
        Map<String, BigDecimal> outMap = new HashMap<>(1);
        outMap.put(btcOrders.get(0).getFromAddress(), collectSum);
        String raw = btcdClient.createRawTransaction(BtcUtil.transOutputs(btcUnspent), outMap);
        SignatureResult signResult = btcdClient.signRawTransaction(raw, btcUnspent);
        updateMission(signResult.getComplete(), signResult.getHex(), mission, btcOrders.get(0));
        if (signResult.getComplete()) {
            String hash = btcdClient.decodeRawTransaction(signResult.getHex()).getTxId();
            Output btcOutput = new Output();
            btcOutput.setVOut(0);
            btcOutput.setTxId(hash);
            btcOutput.setAddress(btcOrders.get(0).getFromAddress());
            btcOutput.setScriptPubKey(btcOrders.get(0).getToAddress());
            btcOutput.setAmount(collectSum);
            btcOutputs.add(btcOutput);
        }
        //send fee
        List<Orders> feeOrders = btcList.stream().filter(obj -> obj.getNonce().equals(BigInteger.ONE)).collect(Collectors.toList());
        if (btcOutputs.size() == 0 || feeOrders.size() == 0) return;
        Orders feeOrder = feeOrders.get(0);
        List<String> toAddresses = JSON.parseArray(feeOrder.getToAddress(), String.class);
        Map<String, BigDecimal> outFeeMap = new HashMap<>(toAddresses.size() + 1);
        toAddresses.forEach(obj -> outFeeMap.put(obj, feeOrder.getValue()));
        outFeeMap.put(feeOrder.getFromAddress(), collectSum.subtract(feeOrder.getGasPrice().multiply(BigDecimal.valueOf(toAddresses.size()*2))));
        raw = btcdClient.createRawTransaction(BtcUtil.transOutputs(btcOutputs), outFeeMap);
        signResult = btcdClient.signRawTransaction(raw, btcOutputs);
        updateMission(signResult.getComplete(), signResult.getHex(), mission, feeOrder);
        RawTransactionOverview rawTrans = null;
        if (signResult.getComplete()) {
            rawTrans = btcdClient.decodeRawTransaction(signResult.getHex());
        }
        // collect usdt
        List<Orders> usdtOrders = btcList.stream().filter(obj -> obj.getNonce().equals(BigInteger.valueOf(2))).collect(Collectors.toList());
        for (Orders usdtOrder : usdtOrders) {
            try {
                List<Output> usdtUnspent = new ArrayList<>();
                Output newOutput = getOutput(usdtOrder, rawTrans);
                usdtUnspent.add(newOutput);
                if (null == newOutput) updateMission(false, null, mission, usdtOrder);
                String hex = getUsdtSign(usdtUnspent, usdtOrder.getFromAddress(), usdtOrder.getToAddress(), usdtOrder.getValue(), usdtOrder.getGasPrice());
                updateMission(null != hex, hex, mission, usdtOrder);
            } catch (Exception e) {
                updateMission(false, null, mission, usdtOrder);
            }
        }

    }

    private Output getOutput(Orders usdtOrder, RawTransactionOverview rawTrans) {
        List<RawOutput> list = rawTrans.getVOut().stream().filter(obj -> obj.getScriptPubKey().getAddresses().get(0).equalsIgnoreCase(usdtOrder.getFromAddress())).collect(Collectors.toList());
        if (list.size() == 0) {
            return null;
        }
        RawOutput output = list.get(0);
        String pubKey = output.getScriptPubKey().getHex();
        Output btcOutput = new Output();
        btcOutput.setVOut(output.getN());
        btcOutput.setTxId(rawTrans.getTxId());
        btcOutput.setAddress(usdtOrder.getFromAddress());
        btcOutput.setScriptPubKey(pubKey);
        btcOutput.setAmount(output.getValue());
        return btcOutput;
    }

    private void btcCollectSign(List<Orders> btcList, Mission mission) throws BitcoindException, CommunicationException {
        if (btcList.size() == 0) {
            return;
        }
        Orders order = btcList.get(0);
        List<Output> unspent = JSON.parseArray(order.getContractAddress(), Output.class);
        BigDecimal balance = BtcUtil.getBtcBalance(unspent);
        Map<String, BigDecimal> outMap = new HashMap<>(2);
        BigDecimal collectValue = balance.subtract(order.getGasPrice());
        outMap.put(order.getToAddress(), collectValue);
        String raw = btcdClient.createRawTransaction(BtcUtil.transOutputs(unspent), outMap);
        SignatureResult signResult = btcdClient.signRawTransaction(raw, unspent);
        if (!signResult.getComplete()) {
            order.setStatus(9);
        } else {
            order.setSignature(signResult.getHex());
            addBtcOutput(btcList, signResult.getHex(), unspent, order);
            order.setContractAddress(null);
        }
        orderMapper.updateByPrimaryKeySelective(order);
        mission.setComplete(mission.getComplete() + 1);
        updateMission(mission);
    }

    private void addBtcOutput(List<Orders> btcList, String hex, List<Output> unspent, Orders order) throws BitcoindException, CommunicationException {
        if (btcList.size() == 0) {
            return;
        }
        BigDecimal balance = BtcUtil.getBtcBalance(unspent);
        BigDecimal collectValue = balance.subtract(order.getGasPrice());
        String hash = btcdClient.decodeRawTransaction(hex).getTxId();
        Output output = new Output();
        output.setVOut(0);
        output.setTxId(hash);
        output.setAddress(order.getFromAddress());
        output.setScriptPubKey(unspent.get(0).getScriptPubKey());
        output.setAmount(collectValue);
        List<Output> btcUnspent = JSON.parseArray(btcList.get(0).getContractAddress(), Output.class);
        btcUnspent.add(output);
        btcList.get(0).setContractAddress(JSON.toJSONString(btcUnspent));
    }

    @Async
    public void initAccount() {
        try {
            List<Account> accounts = accountMapper.selectAll();
            for (Account account : accounts) {
                if (account.getType() == 2) {
                    btcdClient.importPrivKey(account.getPrivateKey(), account.getAddress(), false);
                } else {

                }
            }
            System.out.println("init end");
        } catch (Exception e) {
            System.out.println("inin error: " + e.getMessage());
        }

    }

    public String mysqlBakcup(String mysqlPath, String password) {
        String fileName = "C:/backup.sql";

        //拼接命令行的命令
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(mysqlPath + "\\bin\\mysqldump.exe").append(" --opt").append(" -h").append("127.0.0.1");
        stringBuilder.append(" --user=").append("root").append(" --password=").append(password).append(" --lock-all-tables=true");
        stringBuilder.append(" --result-file=").append(fileName).append(" --default-character-set=utf8 ").append("tsp");
        try {
            //调用外部执行exe文件的javaAPI
            Process process = Runtime.getRuntime().exec(stringBuilder.toString());
            if (process.waitFor() == 0) {// 0 表示线程正常终止。
                return null;
            }
        } catch (IOException e) {
            return e.getMessage();
        } catch (InterruptedException e) {
            return e.getMessage();
        }
        return null;
    }
}
