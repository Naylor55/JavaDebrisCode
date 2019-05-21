package com.naylor.javaserializable;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static com.naylor.javaserializable.domain.Deserialize.jacksonDeserialize;
import static com.naylor.javaserializable.domain.Serializable.jacksonSerializable;

@SpringBootApplication
public class JavaserializableApplication {

    public static void main(String[] args) throws Exception {
        //SpringApplication.run(JavaserializableApplication.class, args);

        System.out.println("DebrisCode---Start");

        /**
         * 序列化
         */
        jacksonSerializable();

        /**
         * 反序列化
         */
        jacksonDeserialize();

        System.out.println("DebrisCode---End");

    }

}
