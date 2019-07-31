package com.atguigu.gmall.auth.util;

import io.jsonwebtoken.*;

import java.util.Map;

public class JwtUtil {

    public static void main(String[] args) {

        // key : 服务器密钥
        // salt : 客户端的信息
        // param ：用户
//        User user = new User();
//        Map<String,Object> map = new HashMap<>();
//        map.put("username","jack");
//        String ip = "127.0.0.1";
//        String token = encode("gmall", map, ip);
        String token = "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VybmFtZSI6ImphY2sifQ.k8F2R4wbzvGWr4ZRlVRYrcd3pRbx5-0XIclVRUEZJ5Q";
        System.out.println(token);

        Map<String, Object> gmall = decode(token, "1231313", "1231323123123123131331313131313131");

        System.out.println(gmall);
    }

    public static String encode(String key, Map<String,Object> param, String salt){
        if(salt!=null){
            key+=salt;
        }
        JwtBuilder jwtBuilder = Jwts.builder().signWith(SignatureAlgorithm.HS256,key);

        jwtBuilder = jwtBuilder.setClaims(param);

        String token = jwtBuilder.compact();
        return token;

    }


    public  static Map<String,Object>  decode(String token ,String key,String salt){
        Claims claims=null;
        if (salt!=null){
            key+=salt;
        }
        try {
            claims= Jwts.parser().setSigningKey(key).parseClaimsJws(token).getBody();
        } catch ( JwtException e) {
           return null;
        }
        return  claims;
    }
}
