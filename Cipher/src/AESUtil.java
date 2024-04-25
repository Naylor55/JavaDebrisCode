

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

import sun.misc.*;

@SuppressWarnings("restriction")
public class AESUtil {

    public static String aesEncrypt(String str, String key) {
        String res="";
        try {
            if (str == null || key == null) return null;
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key.getBytes("utf-8"), "AES"));
            byte[] bytes = cipher.doFinal(str.getBytes("utf-8"));
            res= new BASE64Encoder().encode(bytes);
        } catch (Exception e) {

        } finally {
            return res;
        }
    }

    public static String aesDecrypt(String str, String key) {
        String result="";
        try {
            if (str == null || key == null) return null;
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key.getBytes("utf-8"), "AES"));
            byte[] bytes = new BASE64Decoder().decodeBuffer(str);
            bytes = cipher.doFinal(bytes);
            result= new String(bytes, "utf-8");
        } catch (Exception e) {

        } finally {
            return result;
        }
    }
}