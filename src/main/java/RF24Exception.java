import jpigpio.PigpioException;

/**
 * Created by Jozef on 09.09.2016.
 */
public class RF24Exception extends PigpioException {

    public RF24Exception() {
        super();
    }

    public RF24Exception(String arg0, Throwable arg1, boolean arg2, boolean arg3) {
        super(arg0, arg1, arg2, arg3);
    }

    public RF24Exception(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }

    public RF24Exception(String arg0) {
        super(arg0);
    }

    public RF24Exception(Throwable arg0) {
        super(arg0);
    }

}
