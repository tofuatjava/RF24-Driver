/**
 * Created by Jozef on 09.09.2016.
 */
public class Util {
    public static String bytesToHex(byte[] bytes) {
        final char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String toBinaryString(int a,int bits){
        return String.format("%"+bits+"s", Integer.toBinaryString(a)).replace(' ', '0');
    }

    public static byte[] reverseArray(byte[] list) {
        byte[] result = new byte[list.length];

        for (int i = 0, j = result.length - 1; i < list.length; i++, j--) {
            result[j] = list[i];
        }
        return result;
    }

}
