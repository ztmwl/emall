package com.lujiahao.sso.controller;

import com.lujiahao.common.utils.CookieUtils;
import com.lujiahao.sso.domain.Const;
import com.lujiahao.sso.domain.EDataType;
import com.lujiahao.sso.domain.UserDTO;
import com.lujiahao.sso.service.IUserService;
import com.lujiahao.common.domain.ServerResponse;
import com.lujiahao.common.utils.ExceptionUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 登录
 *
 * @author lujiahao
 * @date 2017/10/17
 */
@Controller
@RequestMapping(value = "/user")
public class SsoController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SsoController.class);

    @Autowired
    private IUserService IUserService;

    /**
     * 校验数据是否可用
     *
     * @param param 需要校验的数据内容
     * @param type  数据类型
     *              callback    是个回调地址,就是这个进入这个页面之前访问的地址
     * @return
     */
    @RequestMapping(value = "/check/{param}/{type}")
    @ResponseBody
    public Object checkData(HttpServletRequest request, @PathVariable String param, @PathVariable Integer type) {
        String callback = request.getParameter("callback");
        ServerResponse result = null;
        // 参数有效性校验
        if (StringUtils.isBlank(param)) {
            result = ServerResponse.build(400, "校验内容不能为空");
        }
        if (type == null) {
            result = ServerResponse.build(400, "校验内容类型不能为空");
        }
        if (type != EDataType.USERNAME.getValue() && type != EDataType.PHONE.getValue() && type != EDataType.EMAIL.getValue()) {
            result = ServerResponse.build(400, "校验内容类型错误");
        }
        // 校验出错
        if (result != null) {
            return checkCallBack(callback, result);
        }
        try {
            // 调用服务
            result = IUserService.checkData(param, type);
        } catch (Exception e) {
            result = ServerResponse.build(500, ExceptionUtil.getStackTrace(e));
            e.printStackTrace();
        }
        return checkCallBack(callback, result);
    }

    /**
     * 解决JSONP跨域请求
     *
     * @param callback 回调地址
     * @param result   返回的JavaBean
     * @return
     */
    private Object checkCallBack(String callback, ServerResponse result) {
        if (callback != null) {
            MappingJacksonValue mappingJacksonValue = new MappingJacksonValue(result);
            mappingJacksonValue.setJsonpFunction(callback);
            return mappingJacksonValue;
        } else {
            return result;
        }
    }

    /**
     * 创建用户
     * TODO 更好的用户体验是登录之后能把用户名带过去,这个以前的那个管理后台可以做到  仿照一下看看
     */
    @RequestMapping(value = "/register", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse createUser(UserDTO userDTO) {
        int resultCount = IUserService.createUser(userDTO);
        if (resultCount > 0) {
            return ServerResponse.success();
        } else {
            return ServerResponse.build(500, "用户注册失败");
        }
    }

    /**
     * 用户登录
     */
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse userLogin(UserDTO userDTO, HttpServletRequest request, HttpServletResponse response) {
        String username = userDTO.getUsername();
        String password = userDTO.getPassword();
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            return ServerResponse.error("用户名或密码为空");
        }
        ServerResponse serverResponse = IUserService.userLogin(username, password);
        if (serverResponse.isSuccess()) {
            // 添加写cookie的逻辑  cookie有效期是关闭浏览器失效
            CookieUtils.setCookie(request, response, Const.COOKIE_TOKEN, serverResponse.getData().toString());
        }
        return serverResponse;
    }

    /**
     * 通过token查询用户信息
     *
     * @param token
     * @return
     */
    @RequestMapping(value = "/token/{token}")
    @ResponseBody
    public Object getUserByToken(@PathVariable String token, String callback) {
        ServerResponse result = null;
        try {
            result = IUserService.getUserByToken(token);
        } catch (Exception e) {
            e.printStackTrace();
            result = ServerResponse.build(500, ExceptionUtil.getStackTrace(e));
        }
        // 判断是否为jsonp调用
        if (StringUtils.isBlank(callback)) {
            // 不是jsonp调用
            return result;
        } else {
            MappingJacksonValue mappingJacksonValue = new MappingJacksonValue(result);
            mappingJacksonValue.setJsonpFunction(callback);
            return mappingJacksonValue;
        }
    }
}
