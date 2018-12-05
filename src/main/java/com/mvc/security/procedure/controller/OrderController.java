package com.mvc.security.procedure.controller;

import com.alibaba.druid.support.json.JSONUtils;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageInfo;
import com.mvc.common.msg.Result;
import com.mvc.common.msg.ResultGenerator;
import com.mvc.security.procedure.bean.*;
import com.mvc.security.procedure.bean.dto.NewAccountDTO;
import com.mvc.security.procedure.bean.dto.Page;
import com.mvc.security.procedure.service.OrderService;
import com.mvc.security.procedure.util.EncryptionUtil;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * order controller
 *
 * @author qiyichen
 * @create 2018/3/10 17:25
 */
@RestController
@RequestMapping("order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @ApiOperation("批量创建账户")
    @PostMapping("account")
    Result newAccount(@RequestBody @Valid NewAccountDTO newAccountDTO) {
        orderService.newAccounts(newAccountDTO);
        return ResultGenerator.genSuccessResult();
    }

    @ApiOperation("删除批量创建账户任务")
    @DeleteMapping("account/{id}")
    Result delAccount(@PathVariable BigInteger id) {
        orderService.delAccount(id);
        return ResultGenerator.genSuccessResult();
    }

    @ApiOperation("获取创建账户任务列表")
    @GetMapping("mission")
    Result getAccountMission(@ModelAttribute Page page) {
        PageInfo<Mission> result = orderService.getMission(1);
        return ResultGenerator.genSuccessResult(result);
    }

    @ApiOperation("获取超级汇总账户, 如果不存在则创建,type[1:以太系]")
    @GetMapping("admin/{type}")
    Result getAdmin(@PathVariable Integer type) throws Exception {
        Account result = orderService.getAdmin(type);
        return ResultGenerator.genSuccessResult(result);
    }

    @ApiOperation("下载账户数据")
    @GetMapping("mission/{id}")
    void getAccount(@PathVariable BigInteger id, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> accountList = orderService.getAccountList(id);
        response.setContentType("text/plain");
        response.addHeader("Content-Disposition", "attachment; filename=" + String.format("account_%s.json", id));
        OutputStream os = response.getOutputStream();
        BufferedOutputStream buff = new BufferedOutputStream(os);
        buff.write(JSONUtils.toJSONString(accountList).getBytes("UTF-8"));
        buff.flush();
        buff.close();
    }

    @ApiOperation(value = "导入待签名记录, 根据订单id去重, 不同系统注意不要重复, 格式为json数组", notes = "[{\"orderId\":\"C01\",\"tokenType\":\"ETH\",\"value\":0.5,\"fromAddress\":\"0xbf281020af1dde1037a7bd741ee52e9966d48793\",\"toAddress\":\"0x45e3dfaa907dd7c2a09d3a1b0002bb07c825094a\"}]")
    @PostMapping("sign")
    Result importOrders(@RequestBody MultipartFile file) throws Exception {
        String jsonStr = IOUtils.toString(file.getInputStream());
        OrderEntity orderEntity = JSON.parseObject(jsonStr, OrderEntity.class);
        String sign = EncryptionUtil.md5(("wallet-shell" + EncryptionUtil.md5(orderEntity.getJsonStr())));
        Assert.isTrue(sign.equalsIgnoreCase(orderEntity.getSign()), "文件错误,请检查");
        List<Orders> list = JSON.parseArray(orderEntity.getJsonStr(), Orders.class);
        orderService.importOrders(list);
        return ResultGenerator.genSuccessResult();
    }

    @ApiOperation("查询签名记录列表")
    @GetMapping("sign")
    Result signList(@ModelAttribute Page page) throws IOException {
        PageInfo<Mission> result = orderService.getMission(2);
        return ResultGenerator.genSuccessResult(result);
    }

    @ApiOperation("下载签名数据")
    @GetMapping("sign/{id}")
    void getSign(@PathVariable BigInteger id, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> accountList = orderService.getSign(id);
        response.setContentType("text/plain");
        response.addHeader("Content-Disposition", "attachment; filename=" + String.format("signature_%s.json", id));
        OutputStream os = response.getOutputStream();
        BufferedOutputStream buff = new BufferedOutputStream(os);
        buff.write(JSONUtils.toJSONString(accountList).getBytes("UTF-8"));
        buff.flush();
        buff.close();
    }

    @ApiOperation("获取当前gas费用, gasLimit只用于eth时为固定21000,因此调整gasprice和gas是挂钩的,请确保比例正确")
    @GetMapping("gas")
    Result<Gas> gasResult() {
        return ResultGenerator.genSuccessResult(OrderService.getGas());
    }

    @ApiOperation("修改Gas费用,临时存储,如果重启服务器需要重新设置")
    @PutMapping("gas")
    Result<Gas> gasResult(@RequestBody Gas gas) {
        return ResultGenerator.genSuccessResult(OrderService.setGas(gas));
    }


}
