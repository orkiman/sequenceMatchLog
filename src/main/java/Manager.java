import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Manager {

    private Serial io, reader1, reader2;
    private SequenceMatchLogGui gui;
    private boolean reader1WaitingForRead, reader2WaitingForRead, ascending;
    private String reader1current, reader2Current, lastReader1;
    private int reader1NoReadCounter, reader2NoReadCounter, maxNoReadAllowed;
    private String noReadString;
    private Set<String> barcodesFromFile;
    public Manager(SequenceMatchLogGui gui) throws IOException {
        this.gui = gui;
        ascending = PropertiesHandler.getProperty("ascending", "false").equals("true");
        maxNoReadAllowed = Integer.parseInt(PropertiesHandler.getProperty("maxNoReadAllowed", "0"));
        io = new Serial("io");
        reader1 = new Serial("reader1");
        if (gui.isReader2Active())
            reader2 = new Serial("reader2");
        noReadString = PropertiesHandler.getProperty("noReadString", "noRead");
    }
    public void fillBarcodesSet(String filePath){
        try {
            barcodesFromFile = getSetFromFile(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public synchronized void serialEvent(String serialName, String data) {
        if (serialName.equals("reader1") || serialName.equals("reader2"))
            newDataArrived(serialName, data);
        else if (serialName.equals("io"))
            newTriggerArrived();
    }

    private void newTriggerArrived() {
        boolean reader2Active = gui.isReader2Active();
        if (reader1WaitingForRead) {
            noReadEvent(reader1);
        }
        if (reader2Active && reader2WaitingForRead) {
            noReadEvent(reader2);
        }
        reader1WaitingForRead = reader2WaitingForRead = true;
        gui.addNewEmptyRowToTable();
//        send triggers
        try {
            reader1.writeBytes(PropertiesHandler.getProperty("triggerString", "<t>").getBytes());
            if (reader2Active)
                reader2.writeBytes(PropertiesHandler.getProperty("triggerString", "<t>").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void noReadEvent(Serial reader) {
        gui.addMassage(reader.serialName + " לא נקרא" + System.lineSeparator());
        gui.updateRow0(reader.serialName, noReadString);
        if (reader.equals(reader1)) {
            if (++reader1NoReadCounter > maxNoReadAllowed) {
                stop();
                gui.addMassage(  "לא נקרא ברצף : " + reader1NoReadCounter +reader.serialName + System.lineSeparator());

//                reader1NoReadCounter = 0;
            }
        } else if (reader.equals(reader2)) {
            if (++reader2NoReadCounter > maxNoReadAllowed) {
                stop();
                gui.addMassage(  "לא נקרא ברצף : " + reader2NoReadCounter +reader.serialName + System.lineSeparator());
//                reader2NoReadCounter = 0;
            }
        }
    }

    private void newDataArrived(String readerName, String data) {
        if (readerName.equals("reader1")) {
            if (reader1WaitingForRead) {
                reader1WaitingForRead = false;
                if (data.equals(noReadString)) {
                    noReadEvent(reader1);
                } else {
//                valid data arrived
                    sequenceCheck(data, lastReader1, reader1NoReadCounter, ascending);
                    if (gui.isReader1FileCheckActive()){
                        inFileCheck(data,barcodesFromFile);
                    }
                    if (gui.isReader2Active() && !reader2WaitingForRead)
                        matchCheck(data, reader2Current);
                    reader1current = data;
                    lastReader1 = data;
                    reader1NoReadCounter = 0;
                }
            } else {
                unexpectedDataError(readerName, data);
            }
        }
        if (readerName.equals("reader2")) {
            if (reader2WaitingForRead) {
                reader2WaitingForRead = false;
                if (data.equals(noReadString)) {
                    noReadEvent(reader2);
                } else{
//                    valid data arrived
                    if (!reader1WaitingForRead)
                        matchCheck(reader1current, data);
                    reader2Current = data;
                    reader2NoReadCounter = 0;
                }
            } else {
                unexpectedDataError(readerName, data);
            }
        }
        gui.updateRow0(readerName, data);
    }

//    private void inFileCheck(String data,Set<String> barcodesFromFile) {
////        complete..
//
//    }

    private void unexpectedDataError(String readerName, String data) {
        gui.addMassage(String.format("התקבלה קריאה (%s) מקורא: %s ללא שליחת טריגר " + System.lineSeparator(), data, readerName));
        gui.addToErrorsSet(data);
    }

    private void matchCheck(String reader1, String reader2) {
        if (!reader1.equals(reader2)) {
            stop();
            gui.addMassage(String.format("אין התאמה %s:%s" + System.lineSeparator(), reader1, reader2));
            gui.addToErrorsSet(reader1);
            gui.addToErrorsSet(reader2);
            gui.refreshTableView();

        }
    }

    private void sequenceCheck(String current, String previous, int noReadCounter, boolean ascending) {
        if (lastReader1 != null) { // don't check on first run
            try {
                int checkDigits = Integer.parseInt(PropertiesHandler.getProperty("reader1CheckDigits", "0"));
                int currentInt = Integer.parseInt(current.substring(checkDigits));
                int previousInt = Integer.parseInt(previous.substring(checkDigits));
                int lastManipulated = ascending ? previousInt + noReadCounter + 1 : previousInt - noReadCounter - 1;
                if (lastManipulated != currentInt) {
                    stop();
                    gui.addToErrorsSet(current);
                    String directionText = PropertiesHandler.getProperty("ascending", "false").equals("true") ? "כיוון עולה" : "כיוון יורד";
                    gui.addMassage(String.format("חוסר רצף %s:%s (%s)" + System.lineSeparator(), current, previous, directionText));
                    gui.refreshTableView();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void stop() {
//        send 1 hex to stop io
        io.writeBytes(new byte[]{1});
    }
    private void inFileCheck(String barcode, Set <String> barcodes){
        if (!barcodes.contains(barcode)){
            gui.addMassage("ברקוד לא בקובץ: " + barcode + System.lineSeparator());
            gui.addToErrorsSet(barcode);
            stop();
        }
    }
    private Set <String> getSetFromFile(String filePath) throws IOException {
        int startPos = Integer.parseInt(PropertiesHandler.getProperty("startPositionInLine_Include","0"));
        int endPos = Integer.parseInt(PropertiesHandler.getProperty("endPositionInLine_Exclude","8"));
        try (Stream<String> lines = Files.lines(Path.of(filePath)))
        {

            Set <String> barcodes = lines
                    .map(s -> s.substring(startPos,endPos))
                    .collect(Collectors.toSet());
//            barcodes.forEach(System.out::println);
            return barcodes;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


}
