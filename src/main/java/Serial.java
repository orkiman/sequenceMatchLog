import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import org.ini4j.Wini;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

/**
 * use this file as a serial template
 * copy "serial.ini" with it!!
 * if desired, change serial.ini section names to reader names (e.g. reader1 to "cardReader")
 * this should match the reader names in "config.properties"
 */
public class Serial {

    SerialPort port;
    String serialName;
    char stxChar, etxChar;
    String bufferedMassage = "";
//    SerialPort port;

    public Serial(String serialName) throws IOException {
        this.serialName = serialName;
        Wini ini = new Wini(new File("serial.ini"));
        int baudrate = ini.get(serialName, "baudrate", int.class);
        int dataBits = ini.get(serialName, "dataBits", int.class);
        String stopBitsString = ini.get(serialName, "stopBits");
        String parityString = ini.get(serialName, "parity");
        String com = ini.get(serialName, "com");
        int stopBits = switch (stopBitsString) {
            case "1" -> SerialPort.ONE_STOP_BIT;
            case "1.5" -> SerialPort.ONE_POINT_FIVE_STOP_BITS;
            case "2" -> SerialPort.TWO_STOP_BITS;
            default -> SerialPort.ONE_STOP_BIT;
        };
        int parity = switch (parityString) {
            case "no" -> SerialPort.NO_PARITY;
            case "odd" -> SerialPort.ODD_PARITY;
            case "even" -> SerialPort.EVEN_PARITY;
            case "mark" -> SerialPort.MARK_PARITY;
            case "space" -> SerialPort.SPACE_PARITY;
            default -> SerialPort.NO_PARITY;
        };
        port = SerialPort.getCommPort(com);
        port.setComPortParameters(baudrate, dataBits, stopBits, parity);
//        System.out.println(baudrate + " " + dataBits + " " + stopBits + " " + parity);
        byte s = ini.get("delimiters", "stx", byte.class);
        stxChar = (char) s;
        byte e = ini.get("delimiters", "etx", byte.class);
        etxChar = (char) e;
        if (!port.openPort()) {
            JOptionPane.showMessageDialog(null, com + " : " + baudrate + ", " + dataBits + ", " + stopBits + ", " + parity, "port open error", JOptionPane.WARNING_MESSAGE);
        }
        listenToPort(port);
//        return port;
    }

    private void listenToPort(SerialPort comPort) {
        comPort.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
            }

            @Override
            public void serialEvent(SerialPortEvent event) {
                byte[] newData = event.getReceivedData();
                bufferedMassage += new String(newData);
//                System.out.println(Arrays.toString(newData));
                int etxIndex;
                do {
                    int stxIndex = bufferedMassage.indexOf(stxChar);
                    etxIndex = bufferedMassage.indexOf(etxChar);
//            process complete massages
                    if (etxIndex != -1) {
                        if (stxIndex != -1 && etxIndex > stxIndex) {
                            processCompleteMassage(bufferedMassage.substring(stxIndex + 1, etxIndex));
                        }
                        bufferedMassage = bufferedMassage.substring(etxIndex + 1);
                    }
                } while (etxIndex != -1);
            }
        });
    }

    private void processCompleteMassage(String completeMassage) {
//        pay attention to concurrecy !!!
        System.out.println(Thread.currentThread().getName() + " :" + completeMassage + ".");
    }
    public void writeBytes(byte[] data){
        port.writeBytes(data,data.length);
    }
}
