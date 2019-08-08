public class Main {

    public static void main(String[] args) {

        System.out.println("Hello World!");

        String s = "领先未来科技集团有限公司";

        System.out.println("明文:" + s);

        String s1 = AESUtil.aesEncrypt(s, "LeadingLeading00");

        System.out.println("密文:" + s1);

        System.out.println("密文解密后的明文:" + AESUtil.aesDecrypt(s1, "LeadingLeading00"));
    }
}
