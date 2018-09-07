package com.mvc.security.procedure.controller;

import com.alibaba.druid.support.json.JSONUtils;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageInfo;
import com.mvc.common.msg.Result;
import com.mvc.common.msg.ResultGenerator;
import com.mvc.security.procedure.bean.Account;
import com.mvc.security.procedure.bean.Mission;
import com.mvc.security.procedure.bean.Orders;
import com.mvc.security.procedure.bean.dto.NewAccountDTO;
import com.mvc.security.procedure.bean.dto.Page;
import com.mvc.security.procedure.service.OrderService;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.web3j.crypto.CipherException;

import javax.servlet.http.HttpServletResponse;
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
    Result newAccount(@RequestBody NewAccountDTO newAccountDTO) {
        orderService.newAccounts(newAccountDTO);
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
    Result importOrders(@RequestBody MultipartFile file) throws IOException {
        String jsonStr = IOUtils.toString(file.getInputStream());
        List<Orders> list = JSON.parseArray(jsonStr, Orders.class);
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

}
