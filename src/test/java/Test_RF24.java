
import jpigpio.*;
import rf24j.RF24;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Test_RF24 {
    private RF24 rf24;
    int cePin = 22;  //GPIO number, e.g. GPIO 22 = PIN 15
    int csnPin = 8;  //GPIO number, e.g. GPIO 8 = PIN 24

    public static void main(String args[]) throws PigpioException {
        System.out.println("Test_RF24");
        Test_RF24 app = new Test_RF24();
        app.run();
    }

    public void run() throws PigpioException {

        System.out.println("Creating pigpio...");
        JPigpio pigpio = new PigpioSocket("pigpiod-host", 8888);

        //JPigpio pigpio = new Pigpio();

        System.out.println("Going to initialize pigpio...");
        pigpio.gpioInitialize();

        System.out.println("Creating RF24...");
        rf24 = new RF24(pigpio);
        p("Initializing...");
        if (!rf24.init(cePin, csnPin)) {
            p("Failed to initialize nRF module. Module not present?");
            rf24.terminate();
            pigpio.gpioTerminate();
            return;
        }

        // 5 byte address width
        rf24.setAddressWidth(5);

        // set remote device address - to which data will be sent and from which data will be received
        byte rcvAddr[] = { 'R', 'C', 'V', '0', '1' };
        // set transmitter device address - from which data will be sent
        byte sndAddr[] = { 'S', 'N', 'D', '0', '1' };

        // following params should be configured the same as the other side
        rf24.setPayloadSize(32); 				// 32 bytes payload
        rf24.setChannel(76);     				// RF channel
        //rf24.setRetries(5,15);   				// 5 retries, 15x250ms delay between retries
        rf24.setCRCLength(2);      				// 16 bit CRC
        rf24.setDataRate(RF24.RF24_1MBPS);	// 1Mbit/s data rate
        rf24.setAutoACK(false);					// expecting automatic acknowledgements from receiver
        rf24.setPALevel(RF24.RF24_PA_LOW);  // low power - testing devices won't be so far apart

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        byte role = 0;
        String cmd = "S";  // start with sender role

        byte counter = 0;
        byte senderData[] = new byte[] {1,2,3,4};
        byte receiverReply[] = new byte[] {99,99,0,0,0,0,0};
        byte data32[] = new byte[32];
        long timeout = 0;

        rf24.clearRegisterBits(RF24.CONFIG_REGISTER,(byte)(1<< RF24.MASK_RX_DR));
        System.out.println(rf24.printDetails());

        showHelp();
        while (role != 3) {

            // read keyboard input
            try {
                if (br.ready())
                    cmd = br.readLine();
            } catch (IOException e) {
                p("IOException happened.");
            }

            switch (cmd.toUpperCase()) {
                case "S":
                    p("***** Switching to SENDER role");
                    cmd = "";
                    role = 0;
                    rf24.stopListening();
                    rf24.powerDown();
                    rf24.openReadingPipe(1,rcvAddr);
                    rf24.openWritingPipe(sndAddr);
                    System.out.println(rf24.printDetails());
                    break;

                case "R":
                    p("***** Switching to RECEIVER role");
                    cmd = "";
                    role = 1;
                    rf24.powerDown();
                    rf24.openReadingPipe(1,sndAddr);
                    rf24.openWritingPipe(rcvAddr);
                    System.out.println(rf24.printDetails());
                    rf24.startListening();
                    break;

                case "E":
                    role = 3;
                    break;

                case "":
                    break;
                default:
                    showHelp();
            }

            // SENDER ROLE ======================
            if (role == 0){
                counter++;
                senderData[0] = counter;
                System.out.print("Sending ... " + Utils.dumpData(senderData) + ": ");

                int result = rf24.write(senderData);
                switch (result) {
                    case 0:
                        p("OK");
                        break;
                    case 1:
                        p("FAILED - max retries");
                        break;
                    case 2:
                        p("FAILED - timeout");
                        break;
                }

                // Handle reply from receiver
                rf24.startListening();  // start listening for reply
                timeout = System.currentTimeMillis() + 3000;

                // wait for reply
                while (!rf24.available() && System.currentTimeMillis() < timeout){
                    System.out.print("w");
                }

                // if reply, then read reply
                if (rf24.available()) {
                    boolean more;
                    p("\nReply: ");
                    do {
                        more = rf24.read(data32);
                        p(Utils.dumpData(data32));
                    } while (more);
                } else
                    p("\nNo reply.");

                rf24.stopListening();

            }

            // RECEIVER ROLE =======================
            if (role == 1) {
                if (rf24.available()) {
                    rf24.read(data32);
                    System.out.println("Received : "+Utils.dumpData(data32));

                    rf24.stopListening();
                    for (byte i=0;i<4;i++)
                        receiverReply[2+i] = data32[i];
                    rf24.write(receiverReply);
                    rf24.startListening();

                } else {
                    p("Waiting for sender..." + counter++);
                    try { Thread.sleep(1000);} catch (InterruptedException e){}
                }

            }

        }

        rf24.terminate();
        pigpio.gpioTerminate();

        System.out.println("Done.");

    }

    private void p(String text) {
        System.out.println(text);
    }

    private void logStatus() throws PigpioException {
        byte status = rf24.readByteRegister(RF24.STATUS_REGISTER);
        p(String.format("status = 0x%x %s", status, rf24.statusToString(status)));
    }

    private void logFifoStatus() throws PigpioException {
        byte status = rf24.readByteRegister(RF24.FIFO_STATUS_REGISTER);
        p(String.format("FIFO Status = 0x%x %s", status, rf24.fifoStatusToString(status)));
    }

    private void logConfig() throws PigpioException {
        byte config = rf24.readByteRegister(RF24.CONFIG_REGISTER);
        System.out.println(String.format("config = %x %s", config, rf24.configToString(config)));
    }

    private void showHelp() {
        p("Type:");
        p("s - to switch to sender mode");
        p("r - to switch to receiver mode");
        p("e - to exit");
    }

}
