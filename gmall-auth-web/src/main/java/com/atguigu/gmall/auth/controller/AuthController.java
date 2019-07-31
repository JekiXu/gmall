package com.atguigu.gmall.auth.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.auth.util.JwtUtil;
import com.atguigu.gmall.beans.UmsMember;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.util.HttpclientUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.RequestWrapper;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class AuthController {

    @Reference
    UserService userService;

    @RequestMapping("vlogin")
    public String vlogin(ModelMap modelMap, HttpServletRequest request) {
        /*
            这里是为自己的网站在微博上注册一个账号，表示可以在微博上使用这个开发平台的东西，
            appId = 1323176429 //自己引用的ID
            secret = 83431bad562099aede6db56601f3b401 //自己引用的秘钥

            这个是用户点击微博登录后跳出来的授权界面，用户点击登录后，
            1：https://api.weibo.com/oauth2/authorize?client_id=1323176429&response_type=code&redirect_uri=http://auth.gmall.com:8085/vlogin

            进行授权后服务器回调的地址，就是本方法，这个url地址中携带了微博平台发送给引用的授权码
            2：http://auth.gmall.com:8085？code=XXXXXXXXXXXXXXXXX

            https://api.weibo.com/oauth2/access_token?client_id=1323176429&client_secret=83431bad562099aede6db56601f3b401&grant_type=authorization_code&redirect_uri=http://auth.gmall.com:8085/vlogin&code=0f4579958644a19160d70b4e2e7956da

         */
        //
        //用户点击授权之后的回调地址，是我们服务端的地址，可以获取用户的一些信息，并且生成一个授权
        Map<String, String> paramMap = new HashMap<>();
        String code = request.getParameter("code");
        paramMap.put("client_id", "1323176429");
        paramMap.put("client_secret", "83431bad562099aede6db56601f3b401");
        paramMap.put("grant_type", "authorization_code");
        paramMap.put("redirect_uri", "http://auth.gmall.com:8085/vlogin");
        paramMap.put("code", code);
        //发送这个请求来向微博平台获取封装了accesstoken的JSON串
        String json = HttpclientUtil.doPost("https://api.weibo.com/oauth2/access_token", paramMap);
        HashMap hashMap = JSON.parseObject(json, new HashMap<String, String>().getClass());

        //通过这个access_token，可以向微博平台发送请求获取用户信息，获取的用户信息，可以保存在自己的数据库中，当做联合数据库中使用，当使用特定的功能的时候，判断该用户的类型进行功能限制
        Object access_token = hashMap.get("access_token");
        Object uid = hashMap.get("uid");
        //获取用户信息
        String userMessage = HttpclientUtil.doGet("https://api.weibo.com/2/users/show.json?access_token=" + access_token + "&uid=" + uid);

        //解析封装了用户信息的JSON串
        HashMap hashMap1 = JSON.parseObject(userMessage, new HashMap<String, String>().getClass());
        System.out.println(hashMap1);
        //对用户进行封装
        UmsMember umsMember = new UmsMember();
        umsMember.setUsername((String) hashMap1.get("name"));
        umsMember.setNickname((String) hashMap1.get("screen_name"));
        umsMember.setAccessCode(code);
        umsMember.setAccessToken(access_token + "");
        umsMember.setSourceUid(Long.parseLong((String) hashMap1.get("idstr")));
        umsMember.setCreateTime(new Date());
        umsMember.setCity((String) hashMap1.get("city"));
        umsMember.setSourceType(2);
        //获取用户信息存如数据库，是联合账户,返回添加的用户
        UmsMember umsMemberDb = userService.addUmsMember(umsMember);

        String remoteIP = request.getHeader("x-forward-for");
        if (remoteIP == null || "".equals(remoteIP)) {
            remoteIP = request.getRemoteAddr();
            if (remoteIP == null || "".equals(remoteIP)) {
                remoteIP = "127.0.0.1";

            }
        }
        String nickname = umsMemberDb.getNickname();
        String memberId = umsMemberDb.getId();

        Map<String,Object> memberMap = new HashMap<>();
        memberMap.put("nickname",nickname);
        memberMap.put("memberId",memberId);
        String token = JwtUtil.encode("gmall", memberMap, remoteIP);
        return "redirect:http://search.gmall.com:8083/index?newToken="+token;
    }

    @RequestMapping("loginPage")
    public String loginPage(String ReturnUrl, ModelMap map) {
        map.put("ReturnUrl", ReturnUrl);
        return "loginPage";
    }

    @RequestMapping("login")
    @ResponseBody
    public String login(UmsMember umsMember, HttpServletRequest request) {
        //验证用户输入的账号和密码是否正确
        UmsMember umsMemberFromDb = userService.getMember(umsMember);
        String nickname = umsMemberFromDb.getNickname();
        String memberId = umsMemberFromDb.getId();

        //通过请求头中的x-forward-for判断该请求是否是转发过来的，比如有可能是nginx转发过来的
        String remoteIP = request.getHeader("x-forward-for");
        if (remoteIP == null || "".equals(remoteIP)) {
            remoteIP = request.getRemoteAddr();
            if (remoteIP == null || "".equals(remoteIP)) {
                remoteIP = "127.0.0.1";
            }
        }
        String token = "";
        if (umsMemberFromDb != null) {
            //使用jwt工具生成一个token，发送到页面上
            Map<String, Object> memberMap = new HashMap<>();
            memberMap.put("nickName", nickname);
            memberMap.put("memberId", memberId);
            token = JwtUtil.encode("gmall", memberMap, remoteIP);
            System.out.println(token);
        }
        return token;
    }

    @RequestMapping("verifi")
    @ResponseBody
    public Map<String, Object> verifi(String token, HttpServletRequest request) {
        Map<String, Object> returnMap = new HashMap<>();
        //判断这个IP是否是转发过来的
        String remoteIP = request.getHeader("x-forward-for");
        if (remoteIP == null || "".equals(remoteIP)) {
            remoteIP = request.getRemoteAddr();
            if (remoteIP == null || "".equals(remoteIP)) {
                remoteIP = "127.0.0.1";
            }
        }
        //校验token是否正确
        Map<String, Object> decode = JwtUtil.decode(token, "gmall", remoteIP);
        if (decode != null) {
            Object nikeName = decode.get("nickName");
            Object memberId = decode.get("memberId");
            returnMap.put("nickName", nikeName);
            returnMap.put("memberId", memberId);
            returnMap.put("succeed", "succeed");
        }
        return returnMap;
    }

}
