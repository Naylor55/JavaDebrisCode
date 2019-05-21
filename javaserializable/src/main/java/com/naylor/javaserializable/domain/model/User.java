package com.naylor.javaserializable.domain.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * @ClassName User
 * @Description
 * @Author Cnayl
 * @Date 2019/5/21 10:55
 * @Version V1.0.0.1
 **/
@Data
@Accessors(chain = true)
public class User implements Serializable {
    private String name;
    private Integer age;
    private Date birthday;
    private String email;
}
