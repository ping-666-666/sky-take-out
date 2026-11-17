package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import com.sky.vo.UserLoginVO;
import lombok.extern.slf4j.Slf4j;                            // [AI 修改 2026-11-17] 记录微信返回的 errcode
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;        // [AI 修改 2026-11-17] 并发首登时的唯一键冲突
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j                                                    // [AI 修改 2026-11-17]
public class UserServiceImpl implements UserService {

    private static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";

    @Autowired
    private WeChatProperties weChatProperties;
    @Autowired
    private UserMapper userMapper;

    public User wxLogin(UserLoginDTO userLoginDTO) {

        String code = userLoginDTO.getCode();
        String openid = getOpenid(code);

        //检验openid是否为null
        if (openid == null) {
            //为null，登录失败，抛出异常
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }
       //检验是否为新用户
       User user =  userMapper.getByOpenid(openid);

        if (user == null) {
            //新用户自动注册
            user = User.builder().
                    openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();

            // ===== [AI 修改 2026-11-17] 开始：openid 加了唯一索引后，并发首登可能有一次插入失败，重查一次即可 =====
            try {
                userMapper.insert(user);
            } catch (DuplicateKeyException e) {
                log.warn("openid={} 已被并发注册，改为查询已存在的用户", openid);
                user = userMapper.getByOpenid(openid);
            }
            // ===== [AI 修改 2026-11-17] 结束 =====
        }
        return user;


    }
    //调用微信接口，获取当前用户的openid
    private String getOpenid(String code) {
        HashMap<String, String> map = new HashMap<>();
        map.put("appid", weChatProperties.getAppid());
        map.put("secret", weChatProperties.getSecret());
        map.put("js_code", code);
        map.put("grant_type", "authorization_code");
        // ===== [AI 修改 2026-11-17] 开始：微信接口异常 / 返回非 JSON / 返回 errcode 时，统一转成"登录失败" =====
        // 原来的写法有两个隐患：
        //   1) doGet 失败时上游可能抛 NPE，json 为空串时 JSON.parseObject("") 返回 null，再 getString 又 NPE；
        //   2) 微信返回 {"errcode":40029,"errmsg":"invalid code"} 时没有 openid，错误原因被丢掉，日志里只剩"登录失败"。
        String json = HttpClientUtil.doGet(WX_LOGIN, map);
        if (json == null || json.trim().isEmpty()) {
            log.error("调用微信 jscode2session 失败，响应为空");
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }

        JSONObject jsonObject;
        try {
            jsonObject = JSON.parseObject(json);
        } catch (Exception e) {
            log.error("微信返回内容不是合法 JSON：{}", json, e);
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }
        if (jsonObject == null) {
            log.error("微信返回内容无法解析：{}", json);
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }

        Integer errcode = jsonObject.getInteger("errcode");
        if (errcode != null && errcode != 0) {
            log.error("微信登录失败 errcode={}, errmsg={}", errcode, jsonObject.getString("errmsg"));
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }

        String openid = jsonObject.getString("openid");
        // ===== [AI 修改 2026-11-17] 结束 =====
        return openid;
    }
}
