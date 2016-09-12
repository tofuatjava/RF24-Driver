package rf24j;

import jpigpio.JPigpio;
import jpigpio.PigpioException;
import jpigpio.WrongModeException;

import java.util.Arrays;

/**
 * Java implementation of Arduino RF24 library (https://maniacbug.github.io/RF24/) for Raspberry Pi.
 * It requires JPigpio library developed by Neil Kolban https://github.com/nkolban/jpigpio
 */

public class RF24 {
    private JPigpio pigpio;

    /**
     * CE which is Chip Enable has nothing to do with CSI.  This pin is driven high for a TX operation
     * and low for a RX operation.
     */
    private int cePin;

    /**
     * CSN is the SPI Slave Select ... we believe CSN = Chip Select NOT.
     */
    private int csnPin;

    private int handle;

    private boolean dynPayloadEnabled = false;
    private int payloadSize = 32;
    private final int MAX_PAYLOAD_SIZE		= 32;


    /* Registers */
    public static final int CONFIG_REGISTER		= 0x00;
    public static final int EN_AA_REGISTER      = 0x01;
    public static final int EN_RXADDR_REGISTER	= 0x02;
    public static final int SETUP_AW_REGISTER	= 0x03;
    public static final int SETUP_RETR_REGISTER	= 0x04;
    public static final int RF_CH_REGISTER		= 0x05;
    public static final int RF_SETUP   			= 0x06;
    public static final int STATUS_REGISTER		= 0x07;
    public static final int OBSERVE_TX = 0x08;
    public static final int RPD        = 0x09;
    public static final int CD         = 0x09;    //CD has been changed to RPD - keeping CD for compatibility reasons
    public static final int RX_ADDR_P0 = 0x0A;
    public static final int RX_ADDR_P1 = 0x0B;
    public static final int RX_ADDR_P2 = 0x0C;
    public static final int RX_ADDR_P3 = 0x0D;
    public static final int RX_ADDR_P4 = 0x0E;
    public static final int RX_ADDR_P5 = 0x0F;
    public static final int TX_ADDR    = 0x10;
    public static final int RX_PW_P0   = 0x11;
    public static final int RX_PW_P1   = 0x12;
    public static final int RX_PW_P2   = 0x13;
    public static final int RX_PW_P3   = 0x14;
    public static final int RX_PW_P4   = 0x15;
    public static final int RX_PW_P5   = 0x16;
    public static final int FIFO_STATUS_REGISTER= 0x17;
    public static final int DYNPD       =0x1C;
    public static final int FEATURE    = 0x1D;

    /* Bit Mnemonics */
    public static final int MASK_RX_DR = 6;
    public static final int MASK_TX_DS = 5;
    public static final int MASK_MAX_RT= 4;
    public static final int EN_CRC     = 3;
    public static final int CRCO       = 2;
    public static final int PWR_UP     = 1;
    public static final int PRIM_RX    = 0;
    public static final int ENAA_P5    = 5;
    public static final int ENAA_P4    = 4;
    public static final int ENAA_P3    = 3;
    public static final int ENAA_P2    = 2;
    public static final int ENAA_P1    = 1;
    public static final int ENAA_P0    = 0;
    public static final int ERX_P5     = 5;
    public static final int ERX_P4     = 4;
    public static final int ERX_P3     = 3;
    public static final int ERX_P2     = 2;
    public static final int ERX_P1     = 1;
    public static final int ERX_P0     = 0;
    public static final int AW         = 0;
    public static final int ARD        = 4;
    public static final int ARC        = 0;

    // RF SETUP
    private static final int CONT_WAVE	 = 7;
    private static final int RF_DR_LOW  = 5;
    private static final int PLL_LOCK   = 4;
    private static final int RF_DR_HIGH = 3;
    private static final int RF_PWR     = 1;
    private static final int LNA_HCURR  = 0;

    public static final int RX_DR      = 6; // RX Data ready status bit
    public static final int TX_DS      = 5; // TX Data ready status bit
    public static final int MAX_RT     = 4; // Maximum number of TX retransmits reached status bit
    public static final int RX_P_NO    = 1;
    public static final int TX_FULL    = 5; // TX FIFO full status bit
    public static final int PLOS_CNT   = 4;
    public static final int ARC_CNT    = 0;
    public static final int TX_REUSE   = 6;
    public static final int FIFO_FULL  = 5;
    public static final int TX_EMPTY   = 4;
    public static final int RX_FULL    = 1;
    public static final int RX_EMPTY   = 0;
    public static final int DPL_P5     = 5;
    public static final int DPL_P4     = 4;
    public static final int DPL_P3     = 3;
    public static final int DPL_P2     = 2;
    public static final int DPL_P1     = 1;
    public static final int DPL_P0     = 0;
    public static final int EN_DPL     = 2;
    public static final int EN_ACK_PAY = 1;
    public static final int EN_DYN_ACK = 0;

    /* Instruction Mnemonics */
    private final int R_REGISTER	= 0x00; // Command to read a register
    private final int W_REGISTER	= 0x20;
    private final int REGISTER_MASK	= 0x1F; // Register is LSB 5 bits 0000 0000 -> 0001 1111
    private final int ACTIVATE		= 0x50;
    private final int R_RX_PL_WID	= 0x60;
    private final int R_RX_PAYLOAD	= 0x61;
    private final int W_TX_PAYLOAD	= 0xA0;
    private final int FLUSH_TX		= 0xE1;
    private final int FLUSH_RX		= 0xE2;
    private final int REUSE_TX_PL	= 0xE3;
    private final byte NOP			= (byte)0xFF;

    // data rates
    public static final int RF24_250KBPS  = 0b10;
    public static final int RF24_1MBPS    = 0b00;
    public static final int RF24_2MBPS    = 0b01;

    // power levels
    public static final int RF24_PA_MIN   = 0b000;
    public static final int RF24_PA_LOW   = 0b010;
    public static final int RF24_PA_HIGH  = 0b100;
    public static final int RF24_PA_MAX   = 0b110;
    public static final int RF24_PA_MASK  = 0b110;

    // address width
    public static final int RF24_AW_3BYTES = 0b01;
    public static final int RF24_AW_4BYTES = 0b10;
    public static final int RF24_AW_5BYTES = 0b11;

    public RF24(JPigpio pigpio) {
        this.pigpio = pigpio;
    }

    /**
     * Initialize nRF24 chip and set the defaults
     * @param cePin gpio pin connected to CE
     * @param csnPin gpio pin connected to CSN
     * @return true if initialized successfully
     * @throws PigpioException
     */
    public boolean init(int cePin, int csnPin) throws PigpioException {
        this.cePin = cePin;
        this.csnPin = csnPin;

        // set specified pins to Output mode
        pigpio.gpioSetMode(cePin, JPigpio.PI_OUTPUT);
        pigpio.gpioSetMode(csnPin, JPigpio.PI_OUTPUT);

        if (pigpio.gpioGetMode(cePin) != JPigpio.PI_OUTPUT)
            throw new WrongModeException(cePin);

        if (pigpio.gpioGetMode(csnPin) != JPigpio.PI_OUTPUT)
            throw new WrongModeException(csnPin);

        ceLow(); // Set the device to RX
        csnHigh(); // Set Slave Select to off
        handle = pigpio.spiOpen(JPigpio.PI_SPI_CHANNEL0, JPigpio.PI_SPI_BAUD_500KHZ, 0);

        byte setupReg = readByteRegister(RF_SETUP);
        // if setup is 0 of 0xff then module does not respond
        if (setupReg == 0 || setupReg == (byte)0xFF)
            return false;

        reset();

        // get status of dynamic payload
        dynPayloadEnabled = ((readByteRegister(FEATURE) & (byte)1<<EN_DPL) == (byte)1<<EN_DPL);

        // Start receiver
        flushRx();
        startListening();

        return true;

    } // End of init

    /**
     * Set initial chip parameters
     * according to nRF24 documentation
     * @throws PigpioException
     */
    public void reset() throws  PigpioException {
        csnLow();
        flushTx();
        csnHigh();

        csnLow();
        flushRx();
        csnHigh();

        // reset registers
        byte a[] = {
                (byte) 1<<EN_CRC, // CONFIG
                (byte) 0b00111111, // EN_AA
                (byte) 0b00000011, // EN_RXADDR
                (byte) 0b00000011, // SETUP_AW
                (byte) 0b00000011, // SETUP_RETR
                (byte) 0b00000010, // RF_CH
                (byte) 0b00001110, // RF_SETUP
                (byte) 0b01110000, // STATUS (clear bits RX_DR, TX_DS, MAX_RT)
        };

        writeRegister(CONFIG_REGISTER,a[0]);
        writeRegister(EN_AA_REGISTER,a[1]);
        writeRegister(EN_RXADDR_REGISTER,a[2]);
        writeRegister(SETUP_AW_REGISTER,a[3]);
        writeRegister(SETUP_RETR_REGISTER,a[4]);
        writeRegister(RF_CH_REGISTER,a[5]);
        writeRegister(RF_SETUP,a[6]);
        writeRegister(STATUS_REGISTER,a[7]);

        byte b[] = {
                0x20,    // RX_PW_P0
                0x20,    // RX_PW_P1
                0x00,    // RX_PW_P2
                0x00,    // RX_PW_P3
                0x00,    // RX_PW_P4
                0x00     // RX_PW_P5
        };

        writeRegister(RX_PW_P0, b[0]);
        writeRegister(RX_PW_P1, b[1]);
        writeRegister(RX_PW_P2, b[2]);
        writeRegister(RX_PW_P3, b[3]);
        writeRegister(RX_PW_P4, b[4]);
        writeRegister(RX_PW_P5, b[5]);

        writeRegister(DYNPD, (byte) 0);

        writeRegister(FEATURE, (byte) 0);

        powerDown();

    }


    /**
     * Tell NRF24 to start listening. Power up and set to RX Mode.
     * Flushes both RX and TX FIFOs. Resets STATUS register flags.
     * @throws PigpioException
     */
    public void startListening() throws PigpioException {
        byte confReg = readByteRegister(CONFIG_REGISTER);
        writeRegister(CONFIG_REGISTER, (byte)(confReg | BV(PWR_UP) | BV(PRIM_RX)) );
        writeRegister(STATUS_REGISTER, (byte)(BV(RX_DR) | BV(TX_DS) | BV(MAX_RT)) );

        //pigpio.gpioDelay(2,JPigpio.PI_MILLISECONDS); // 1.5ms to start

        // Flush buffers
        //flushRx();
        //flushTx();

        // Start listening now
        ceHigh();

        // wait for the radio to come up (130us actually only needed)
        //pigpio.gpioDelay(200, JPigpio.PI_MICROSECONDS);
    }

    /**
     * Stops receiving. Go from active RX Mode to to Standby-I Mode.
     * Flushes both RX and TX FIFOs.
     * @throws PigpioException
     */
    public void stopListening() throws PigpioException {
        ceLow();
        //flushTx();
        //flushRx();
    }

    /**
     * Send data packet - no checking for if sending was successful is done. <br/>
     * Use method write for more complex approach.
     * @param data data to be sent
     * @throws PigpioException
     */
    public void startWrite(byte[] data) throws PigpioException{

        // power up (PWR_UP=1) and set to transmit mode (PRIM_RX=0)
        byte cfg = readByteRegister(CONFIG_REGISTER);
        writeRegister(CONFIG_REGISTER, (byte)(( cfg | BV(PWR_UP) ) & ~BV(PRIM_RX) ) );
        pigpio.gpioDelay(150); // wait for settling the chip
        //pigpio.gpioDelay(2,JPigpio.PI_MILLISECONDS); // 1.5ms to start if in power-down mode

        // Send the payload
        nrfSpiWrite(W_TX_PAYLOAD, data);   // Write to TX FIFO register

        // flash CE=1 for more than 15us to send the packet
        pigpio.gpioTrigger(cePin,20,true);

    }

    /**
     * Send data packet.
     * This is a blocking call, but 60ms max, so no big deal.
     * @param value data to send
     * @return 0 if OK, 1 if number of retries reached, 2 if timeout occurred
     * @throws PigpioException
     */
    public int write(byte[] value) throws PigpioException {
        byte[] buff = value.clone();
        byte status;
        int result = 0;

        ceLow();
        // if fixed payload size and value is shorter than payload size
        if (!dynPayloadEnabled && (buff.length < payloadSize) )
            buff = Arrays.copyOf(buff,payloadSize); // then extend to payload size

        startWrite(buff);

        long timeout = System.currentTimeMillis() + 500;

        // wait for successful transmit (TX_DS=1) or max-retries (MAX_RT=1) or timeout to happen
        do {
            status = readByteRegister(STATUS_REGISTER);
        } while ( (status & (byte)( BV(TX_DS) | BV(MAX_RT) )) == 0 && System.currentTimeMillis() < timeout);

        if ((status & BV(TX_DS)) == 0)
            if ((status & BV(MAX_RT)) > 0 )
                result = 1; // max number of retries reached
            else
                result = 2; // send timeout

        // result of write operation is captured so we can reset TX_DS & MAX_RT bits
        writeRegister(STATUS_REGISTER,(byte)( status | BV(TX_DS) | BV(MAX_RT)));

        //TODO: Handle ACK payload

        powerDown();

        return result;
    }

    /**
     * Checks if data is available for reading in RX FIFO
     * @return true if there is data available for reading
     * @throws PigpioException
     */
    public boolean available() throws PigpioException {
        // See note in getData() function - just checking RX_DR isn't good enough
        byte status = readByteRegister(STATUS_REGISTER);

        if ((status & BV(RX_DR)) != 0) {
            clearRegisterBits(STATUS_REGISTER, BV(RX_DR));
            return true;
        }

        // We can short circuit on RX_DR, but if it's not set, we still need
        // to check the FIFO for any pending packets
        return (readByteRegister(FIFO_STATUS_REGISTER) & BV(RX_EMPTY)) == 0;
    } // End of dataReady

    /**
     * Read payload.<br/>
     * Read should be repeated until method returns false.
     * @param data array to store data into
     * @return true if there is no more data available
     * @throws PigpioException
     */
    public boolean read( byte data[]) throws PigpioException {
        // Fetch the payload
        nrfSpiWrite(R_RX_PAYLOAD, data); // Read payload
        setRegisterBits(STATUS_REGISTER,BV(RX_DR)); // clear RX_DR

        // was this the last of the data available? if RX_EMPTY == 1 => no more data to read
        return (readByteRegister(FIFO_STATUS_REGISTER) & BV(RX_EMPTY)) == 0;
    }

    /**
     * Open pipe for writing
     * @param address address to use as source address when sending data.<br/>
     * Address should be provided with LSB first.
     * @throws PigpioException
     */
    public void openWritingPipe(byte[] address) throws PigpioException {
        // Note that AVR 8-bit uC's store this LSB first, and the NRF24L01(+)
        // expects it LSB first too, so we're good.

        writeRegister(RX_ADDR_P0,address);
        writeRegister(TX_ADDR, address);
        writeRegister(RX_PW_P0, (byte)payloadSize);

    }

    /**
     * Open pipe for reading
     * @param pipe pipe to open (0-5)
     * @param address address to expect data from.<br/>
     * Address should be LSB first.
     * @throws PigpioException
     */
    public void openReadingPipe(int pipe, byte[] address) throws PigpioException {
        if (pipe < 0 || pipe > 5)
            throw new RF24Exception();
        setRegisterBits(EN_RXADDR_REGISTER, BV(pipe));
        writeRegister(RX_ADDR_P0+pipe, address);
    }

    /**
     * Configure delay between retransmissions and number of retransmissions.<br/><br/>
     * Set 1500uS (minimum for 32B payload in ESB@250KBPS) timeouts, to make testing a little easier <br/>
     * WARNING: If this is ever lowered, either 250KBS mode with AA is broken or maximum packet
     * sizes must never be used. See documentation for a more complete explanation.
     *
     * @param delay How long to wait between each retry, in multiples of 250us, max is 15. 0 means 250us, 15 means 4000us.*
     * @param count Number of retries (0 = no retries, max 15 retries)
     * @throws PigpioException
     */
    public void setRetries(int delay, int count) throws PigpioException {
        writeRegister(SETUP_RETR_REGISTER,(byte)((delay & 0x0F) << ARD | (count & 0xf)<<ARC));
    }


    /**
     * Set frequency channel nRF24 operates on
     * @param ch channel 0-127
     * @throws PigpioException
     */
    public void setChannel(int ch) throws PigpioException {
        writeRegister(RF_CH_REGISTER, (byte)(ch & 0x7F)); // max 127 = 0x7F
    }

    /**
     * Set static payload (packet) size for all pipes. <br/>
     * Pipe has to be reopened for reading in order to change payload size.
     * @param size payload size 1-32 bytes
     * @throws PigpioException
     */
    public void setPayloadSize(int size) {
        if (size>0 && size <33)
            this.payloadSize = size;
        else
            this.payloadSize = 32;
    }

    /**
     * Get static payload size.
     * @return
     */
    public int getPayloadSize() {
        return payloadSize;
    }

    //TODO: public int getDynamicPayloadSize()

    //TODO: public void enableACKPayload()

    //TODO: public void enableDynamicPayload()

    //TODO: public boolean isPVariant()

    /**
     * Enable or disable automatic packet acknowledgements. Default is ENABLE.
     * @param enable true to enable, false to disable
     * @throws PigpioException
     */
    public void setAutoACK(boolean enable) throws PigpioException{
        if (enable)
            writeRegister(EN_AA_REGISTER,(byte)0b00111111);
        else
            writeRegister(EN_AA_REGISTER,(byte)0);
    }

    /**
     * Enable or disable automatic packet acknowledgements for specific pipe.
     * @param pipe true to enable, false to disable
     * @param enable
     * @throws PigpioException
     */
    public void setAutoACK(int pipe, boolean enable) throws PigpioException{
        if (enable)
            setRegisterBits(EN_AA_REGISTER,(byte)(1<<pipe));
        else
            clearRegisterBits(EN_AA_REGISTER,(byte)(1<<pipe));
    }

    /**
     * Sets TX output power level.
     * @param level TX output power level
     *              RF24_PA_MIN = -18db
     *              RF24_PA_LOW = -12db
     *              RF24_PA_HIGH = -6db
     *              RF24_PA_MAX = 0db
     * @throws PigpioException
     */
    public void setPALevel(int level) throws PigpioException{
        byte setupReg = readByteRegister(RF_SETUP);
        byte newValue = setupReg;

        newValue = (byte)(newValue & (~RF24_PA_MASK));
        newValue = (byte)(newValue | level);

        writeRegister(RF_SETUP, newValue);
    }

    //TODO: public int getPALevel()

    /**
     * Sets data rate
     * @param dataRate datarate to use
     * 				RF24_250KBPS = 250 kbit/s
     * 				RF24_1MBPS = 1 Mbit/s
     * 				RF24_2MBPS = 2 Mbit/2*
     * @throws PigpioException
     */
    public void setDataRate(int dataRate) throws PigpioException{
        byte setupReg = readByteRegister(RF_SETUP);
        byte newValue = setupReg;

        if ((dataRate & 0b10) == 0b10)
            newValue = (byte)(newValue | 1<<RF_DR_LOW);
        else
            newValue = (byte)(newValue & (~(1<<RF_DR_LOW)));

        if ((dataRate & 0b01) == 0b01)
            newValue = (byte)(newValue | (1<<RF_DR_HIGH));
        else
            newValue = (byte)(newValue & (~(1<<RF_DR_HIGH)));

        writeRegister(RF_SETUP, newValue);

    }

    //TODO: public int getDataRate()

    /**
     * Set CRC length
     * @param length CRC length in bytes (0 = disable, 1 = 8 bits, 2 = 16 bits)
     * @throws PigpioException
     */
    public void setCRCLength(int length) throws PigpioException {
        switch (length){
            case 0:
                clearRegisterBits(CONFIG_REGISTER, (byte)(1<<EN_CRC));  // disable CRC
                break;
            case 1:
                setRegisterBits(CONFIG_REGISTER, (byte)(1<<EN_CRC));    // enable CRC
                clearRegisterBits(CONFIG_REGISTER, (byte)(1<<CRCO));    // zero = 1 byte = 8 bit CRC
                break;
            case 2:
                setRegisterBits(CONFIG_REGISTER, (byte)(1<<EN_CRC));    // enable CRC
                setRegisterBits(CONFIG_REGISTER, (byte)(1<<CRCO));      // one = 2 bytes = 16 bit CRC
                break;
        }

    }

    /**
     * Return CRC length in bytes
     * @return CRC length in bytes
     * @throws PigpioException
     */
    public int getCRCLength() throws PigpioException {
        int l = 1;
        if ((readByteRegister(CONFIG_REGISTER) & (byte)(1<<CRCO)) != 0)
            l = 2;

        return l;
    }


    /**
     * Disable CRC
     * @throws PigpioException
     */
    public void disableCRC() throws PigpioException {
        setCRCLength(0);
    }


    /**
     * Return detailed information about nRF24 chip
     * @return String containing information
     */
    public String printDetails(){
        String p = "";

        try {
            byte statusReg = readByteRegister(STATUS_REGISTER);
            p = "STATUS          = 0x" + String.format("%02x",statusReg) + "  " + String.format("%8s", Integer.toBinaryString(statusReg & 0xFF)).replace(' ', '0');

            byte rxAddr[][] = new byte[6][5];
            readRegister(RX_ADDR_P0,rxAddr[0]);
            readRegister(RX_ADDR_P1,rxAddr[1]);
            readRegister(RX_ADDR_P2,rxAddr[2]);
            readRegister(RX_ADDR_P3,rxAddr[3]);
            readRegister(RX_ADDR_P4,rxAddr[4]);
            readRegister(RX_ADDR_P5,rxAddr[5]);
            // addresses are stored LSB first, so we need to reverse it in order to print it correctly
            p += "\nRX_ADDR_P0-1    = 0x"+ Util.bytesToHex(Util.reverseArray(rxAddr[0]))+ "  0x" + Util.bytesToHex(Util.reverseArray(rxAddr[1]));

            p += "\nRX_ADDR_P2-5    = ";
            for(int i = 2;i<6;i++)
                p += "0x"+String.format("%02x",rxAddr[i][0])+"  ";

            byte txAddr[] = new byte[5];
            readRegister(TX_ADDR,txAddr);
            p += "\nTX_ADDR         = 0x"+ Util.bytesToHex(Util.reverseArray(txAddr));

            byte pwReg[] = {0,0,0,0,0,0};
            readRegister(RX_PW_P0,pwReg);  // read 6 registers/bytes, starting with RX_PW_P0
            p += "\nRX_PW_P0-6      = ";
            for(byte b:pwReg)
                p += "0x"+String.format("%02x",b)+"  ";
            p += "\n";


            byte enAA = readByteRegister(EN_AA_REGISTER);
            p += "\nEN_AA           = 0x"+ String.format("%02x",enAA) + "  " + String.format("%8s", Integer.toBinaryString(enAA & 0xFF)).replace(' ', '0');

            byte enRX = readByteRegister(EN_RXADDR_REGISTER);
            p += "\nEN_RXADDR       = 0x"+ String.format("%02x",enRX) + "  " + String.format("%8s", Integer.toBinaryString(enRX & 0xFF)).replace(' ', '0');

            byte rfChannel = readByteRegister(RF_CH_REGISTER);
            p += "\nRF_CH           = 0x"+ String.format("%02x",rfChannel);

            byte setupReg = readByteRegister(RF_SETUP);
            p += "\nRF_SETUP        = 0x"+ String.format("%02x",setupReg) + "  " + String.format("%8s", Integer.toBinaryString(setupReg & 0xFF)).replace(' ', '0');

            byte configReg = readByteRegister(CONFIG_REGISTER);
            p += "\nCONFIG          = 0x"+ String.format("%02x",configReg) + "  " + String.format("%8s", Integer.toBinaryString(configReg & 0xFF)).replace(' ', '0');

            byte dyn = readByteRegister(DYNPD);
            byte feature = readByteRegister(FEATURE);
            p += "\nDYNPD/FEATURE   = 0x"+ String.format("%02x",dyn)+" 0x"+ String.format("%02x",feature);

            byte dataRate = 0;
            if ((setupReg & 1<<RF_DR_LOW) > 0) dataRate += 2;
            if ((setupReg & 1<<RF_DR_HIGH) > 0) dataRate += 1;
            p += "\nData Rate       = " + dataRateToString(dataRate);

            p += "\nMODEL           = ???";

            p += "\nCRC Length      = ";
            if ((configReg & (1<<CRCO)) == (1<<CRCO) )
                p += "16 bits";
            else
                p += "8 bits";

            p += "\nPA Power        = ???";

            byte retry = readByteRegister(SETUP_RETR_REGISTER);
            p += "\nSETUP_RETR      = 0x"+ String.format("%02x",retry) + "  " + String.format("%8s", Integer.toBinaryString(retry & 0xFF)).replace(' ', '0');

            p += "\n";

            p += setupRetrToString(readByteRegister(SETUP_RETR_REGISTER));
            p += "\nRF Channel      = " + rfChannel;
            p += "\nAddress widths  = " + getAddressWidth() + " bytes";

        } catch (PigpioException e) {
            p += e.getMessage();
        }

        return p+"\n";
    }

    /**
     * Enter Power-down Mode
     * @throws PigpioException
     */
    public void powerDown() throws PigpioException {
        clearRegisterBits(CONFIG_REGISTER,(byte)(1<<PWR_UP));
    }

    /**
     * Leave low-power mode - make radio more reponsive
     * @throws PigpioException
     */
    public void powerUp() throws PigpioException {
        setRegisterBits(CONFIG_REGISTER,(byte)(1<<PWR_UP));
    }

    //TODO: public boolean available(int pipe)

    //TODO: public void writeAckPayload

    //TODO: public boolean isAckPayloadAvailable

    //TODO: whatHappened

    //TODO: testCarrier

    /**
     * Test whether a signal (carrier or otherwise) greater than or equal to -64dBm is present on the channel. Valid only on nRF24L01P (+) hardware.
     * Useful to check for interference on the current channel and channel hopping strategies.
     * @return true if signal => -64dBm, false if not
     * @throws PigpioException
     */
    public boolean testRPD() throws PigpioException {
        return (readByteRegister(RPD) & 1) == 1;
    }

    //############################################################################################
    //############################################################################################

    /**
     * Set RX/TX address width.
     * @param width Width in bytes. Allowed values are 3,4,5.
     * @throws PigpioException
     */
    public void setAddressWidth(int width) throws PigpioException {
		/* Initialize with NOP so we get the first byte read back. */
        switch (width){
            case 3:
                width = RF24_AW_3BYTES;
                break;
            case 4:
                width = RF24_AW_4BYTES;
                break;
            case 5:
                width = RF24_AW_5BYTES;
                break;
            default:
                width = RF24_AW_5BYTES;
        }
        writeRegister(SETUP_AW_REGISTER, (byte)width);
    }

    /**
     * Return address width in bytes
     * @return address width in bytes (3-5)
     * @throws PigpioException
     */
    public int getAddressWidth() throws PigpioException {
        byte w = readByteRegister(SETUP_AW_REGISTER);
        int l = -1;
        switch (w & 0b11){
            case RF24_AW_3BYTES:
                l=3;
                break;
            case RF24_AW_4BYTES:
                l=4;
                break;
            case RF24_AW_5BYTES:
                l=5;
                break;
        }
        return l;
    }

    /**
     * Terminate connection to nRF24 chip
     * @throws PigpioException
     */
    public void terminate() throws PigpioException {
        ceLow();
        powerDown();
    }

    //############################################################################################
    //############################################################################################


    /**
     * Set a bit
     * @param bit
     * @return An integer with the specific bit set.
     */
    private byte BV(int bit) {
        return (byte)(1 << bit);
    } // End of BV

    private void nrfSpiWrite(int reg, byte data[]) throws PigpioException {

        csnLow();
        byte regData[] = { (byte)reg };
        pigpio.spiXfer(handle, regData, regData);
        if (data != null) {
            pigpio.spiXfer(handle, data, data);
        }
        csnHigh();

        // TODO: why is following delay here? a relic? 100ms is quite a long wait...
        // pigpio.gpioDelay(100, JPigpio.PI_MILLISECONDS);
        // TODO: tried to remove 100ms delay, but gpiod stopped responding after 100-200 calls - introduced 1ms delay and it seems to work again
        pigpio.gpioDelay(1,JPigpio.PI_MILLISECONDS);

    }

    /**
     * Move device from mode Standby-I to RX or TX mode - depending on PRIM_RX
     * @throws PigpioException
     */
    private void ceHigh() throws PigpioException {
        pigpio.gpioWrite(cePin, JPigpio.PI_HIGH);
    }

    /**
     * Put the device in a RX state
     * @throws PigpioException
     */
    private void ceLow() throws PigpioException {
        pigpio.gpioWrite(cePin, JPigpio.PI_LOW);
    }

    /**
     * Disable the device (Slave Select)
     * @throws PigpioException
     */
    private void csnHigh() throws PigpioException {
        pigpio.gpioWrite(csnPin, JPigpio.PI_HIGH);
    }

    /**
     * Enable the device (Slave Select)
     * @throws PigpioException
     */
    private void csnLow() throws PigpioException {
        pigpio.gpioWrite(csnPin, JPigpio.PI_LOW);
    }

    /**
     * Flush RX FIFO
     * @throws PigpioException
     */
    private void flushRx() throws PigpioException {
        nrfSpiWrite(FLUSH_RX, null);
    }

    /**
     * Flush TX FIFO
     * @throws PigpioException
     */
    private void flushTx() throws PigpioException {
        nrfSpiWrite(FLUSH_TX, null);
    }

    // ####################################################################################

    /**
     * Reads single one-byte register
     * @param reg register to read
     * @return register value
     * @throws PigpioException
     */
    public byte readByteRegister(int reg) throws PigpioException{
        byte data[] = {NOP};
        readRegister(reg,data);
        return data[0];
    }

    /**
     * Reads an array of bytes from the given start position in the MiRF registers.
     * @param reg starting register
     * @param value array of bytes to store read values to
     * @throws PigpioException
     */
    public void readRegister(int reg, byte value[]) throws PigpioException {
        nrfSpiWrite((R_REGISTER | (REGISTER_MASK & reg)), value);
    } // End of readRegister

    /**
     * Writes provided bytes into MiRF registers starting from provided register address.<br/>
     * Good for multi-byte registers. For single-byte registers you can use method writeByteRegister.
     * @param reg starting register
     * @param data
     * @throws PigpioException
     */
    public void writeRegister(int reg, byte data[]) throws PigpioException {
        //System.out.println("Write register: " + reg + ", mask = " + (W_REGISTER | (REGISTER_MASK & reg)) );

        // The register value will be 32 + reg number as the coding of writing a register is
        // 0b001x xxxx where "xxxxx" is the 5 bit register number.
        byte clonedData[] = data.clone();
        nrfSpiWrite((W_REGISTER | (REGISTER_MASK & reg)), clonedData);
    } // End of writeRegister

    /**
     * Writes value to single-byte register. For multi-byte registers you can use method writeRegister.
     * @param reg register to write to
     * @param value new register value
     * @throws PigpioException
     */
    public void writeRegister(int reg, byte value) throws PigpioException {
        byte data[] = {value};
        writeRegister(reg, data);
    }

    /**
     * Set specified bits in single-byte register
     * @param reg register to set
     * @param bits bits to set
     * @throws PigpioException
     */
    public void setRegisterBits(int reg, byte bits) throws PigpioException {
        byte regVal = readByteRegister(reg);
        byte newVal = regVal;
        newVal = (byte)(newVal | bits);
        writeRegister(reg,newVal);
    }

    /**
     * Clear specified bits in single-byte register
     * @param reg register to set
     * @param bits bts to clear
     * @throws PigpioException
     */
    public void clearRegisterBits(int reg, byte bits) throws PigpioException {
        byte regVal = readByteRegister(reg);
        byte newVal = regVal;
        newVal = (byte)(newVal & ~bits);
        writeRegister(reg,newVal);
    }

    // ####################################################################################


    public String statusToString(byte status) {
        String ret = "";
        ret += "RX_DR: " + bitSet(status, RX_DR);
        ret += ", TX_DS: " + bitSet(status, TX_DS);
        ret += ", MAX_RT: " + bitSet(status, MAX_RT);
        ret += ", RX_P_NO: " + ((status & 0b1110) >> 1);
        ret += ", TX_FULL: " + bitSet(status, TX_FULL);
        return ret;
    }

    public  String configToString(byte value) {
        String ret = "";
        ret += "RX IRQ allowed: " + bitSet(value, MASK_RX_DR);
        ret += ", TX IRQ allowed: " + bitSet(value, MASK_TX_DS);
        ret += ", Max retransmits IRQ allowed: " + bitSet(value, MASK_MAX_RT);
        ret += ", CRC enabled: " + bitSet(value, EN_CRC);
        ret += ", CRC encoding scheme: " + bitSet(value, CRCO);
        ret += ", Power up: " + bitSet(value, PWR_UP);
        ret += ", RX(1)/TX(0): " + bitSet(value, PRIM_RX);
        return ret;
    }

    public String setupRetrToString(byte value) {
        String ret = "";
        ret += "Delay: " + (((value & 0b11110000) >> 4) * 250 + 250);
        ret += ", Auto retransmit count: " + (value & 0b1111);
        return ret;
    }

    private String bitSet(byte value, int bit) {
        return ((value & BV(bit))!=0)?"1":"0";
    }

    public String fifoStatusToString(byte value) {
        String ret = "";
        ret += "Reuse: " + bitSet(value, TX_REUSE);
        ret += ", TX Full: " + bitSet(value, TX_FULL);
        ret += ", TX Empty: " + bitSet(value, TX_EMPTY);
        ret += ", RX Full: " + bitSet(value, RX_FULL);
        ret += ", RX Empty: " + bitSet(value, RX_EMPTY);
        return ret;
    }

    public String dataRateToString(int dataRate) {
        switch (dataRate) {
            case RF24_1MBPS:
                return "1 Mbps";
            case RF24_2MBPS:
                return "2 Mbps";
            case RF24_250KBPS:
                return "256 kbps";
            case 0x11:
                return "reserved";
        }
        return "???";
    }

}
