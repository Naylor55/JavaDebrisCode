package com.naylor.javaserializable.domain;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naylor.javaserializable.domain.model.User;

import java.util.List;

/**
 * @ClassName Deserialize
 * @Description
 * @Author Cnayl
 * @Date 2019/5/21 11:00
 * @Version V1.0.0.1
 **/
public class Deserialize {

    /**
     * 使用 jackson 将  json 字符串反序列化成 Java 对象、泛型
     * @throws Exception
     */
    public static void jacksonDeserialize() throws Exception {
        String json = "{\"name\":\"zhangsan\",\"age\":20,\"birthday\":844099200000,\"email\":\"zhangsan@163.com\"}";
        /**
         * ObjectMapper支持从byte[]、File、InputStream、字符串等数据的JSON反序列化。
         */
        ObjectMapper mapper = new ObjectMapper();
        User user = mapper.readValue(json, User.class);
        System.out.println(user);

        String json2 = "[{\"name\":\"zhangsan\",\"age\":20,\"birthday\":844099200000,\"email\":\"zhangsan@163.com\"}]";
        List<User> beanList = mapper.readValue(json2, new TypeReference<List<User>>() {});
        System.out.println(beanList);
    }
}
