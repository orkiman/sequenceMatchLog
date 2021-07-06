import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    private int digitOnePositionInBarcode;
    private Input[] inputs = new Input[12];
    private List<Cell> cellsList = Collections.synchronizedList(new ArrayList<Cell>());
    private int materialFeederPosition;
    private int checkerPosition;
    boolean feederEyeEventOnRising;
    boolean advanceCycleInputEventOnRising;
    boolean checkerEyeEventOnRising;
    int feederEyeInputNumber, advanceCycleInputNumber, checkerEyeInputNumber;

    public Manager(SequenceMatchLogGui gui) throws IOException {
        this.gui = gui;
        ascending = PropertiesHandler.getProperty("ascending", "false").equals("true");
        maxNoReadAllowed = Integer.parseInt(PropertiesHandler.getProperty("maxNoReadAllowed", "0"));
        io = new Serial("io", this);
        reader1 = new Serial("reader1", this);
        if (gui.isReader2Active())
            reader2 = new Serial("reader2", this);
        noReadString = PropertiesHandler.getProperty("noReadString", "noRead");
        digitOnePositionInBarcode = Integer.parseInt(PropertiesHandler.getProperty("digitOnePositionInBarcode", "0"));
        materialFeederPosition = Integer.parseInt(PropertiesHandler.getProperty("materialFeederLocation", "1"));
        checkerPosition = Integer.parseInt(PropertiesHandler.getProperty("checkerPosition", "3"));
        initInputs();
        initCells();
        feederEyeEventOnRising = PropertiesHandler.getProperty("feederEyeEventOnRising", "true").equals("true");
        advanceCycleInputEventOnRising = PropertiesHandler.getProperty("advanceCycleInputEventOnRising", "true").equals("true");
        checkerEyeEventOnRising = PropertiesHandler.getProperty("checkerEyeEventOnRising", "true").equals("true");
        feederEyeInputNumber = Integer.parseInt(PropertiesHandler.getProperty("feederEyeInputNumber", "7"));
        advanceCycleInputNumber = Integer.parseInt(PropertiesHandler.getProperty("advanceCycleInputNumber", "8"));
        checkerEyeInputNumber = Integer.parseInt(PropertiesHandler.getProperty("checkerEyeInputNumber", "9"));
    }

    private void initInputs() {
        for (int i = 0; i < 12; i++) {
            inputs[i] = new Input();
        }
    }

    private void initCells() throws IOException {
//        cellsList will iterate from position 1 and not 0 for convinious
//        therfor, cellsList length will be checkerposition+1
        for (int i = 0; i < Integer.parseInt(PropertiesHandler.getProperty("checkerPosition", "3")) + 1; i++) {
            cellsList.add(new Cell());
        }
    }

    public void fillBarcodesSet(String filePath) {
        try {
            barcodesFromFile = getSetFromFile(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void serialEvent(String serialName, String data) {
        if (serialName.equals("reader1") || serialName.equals("reader2"))
            newDataArrivedFromReader(serialName, data);
        else if (serialName.equals("io")) {
            ioEvent(data);
        }
    }

    private void ioEvent(String data) {
//        update Inputs State:
        int inputsState = Integer.parseInt(data);
        for (int i = 0; i < 12; i++) {
//            int i1 = inputsState & (1 << i);
            inputs[i].update((inputsState & (1 << i)) != 0);
        }
        Input feederEye = inputs[feederEyeInputNumber];
        Input advanceCycleInput = inputs[advanceCycleInputNumber];
        Input checkerEye = inputs[checkerEyeInputNumber];
        //        ***feeder eye*** INPUT 9
        if (feederEyeEventOnRising && feederEye.rising || !feederEyeEventOnRising && feederEye.falling) {
            newTriggerArrived();
//            fill cells arrray with material :
            Cell feederCell = cellsList.get(materialFeederPosition);
            feederCell.exist = true;
            feederCell.data = "waiting for read";
        }
//        advanceCycleInput finger : INPUT 10
        if (advanceCycleInputEventOnRising && advanceCycleInput.rising || !advanceCycleInputEventOnRising && advanceCycleInput.falling) {
//            check if checker worked correcrtly :
//           todo : discard beggining ???
            Cell checkerCell = cellsList.get(checkerPosition);
            if (checkerCell.exist && !checkerCell.arrivedToChecker) {
//                checker didn't reset cell - error !
                stop();
                gui.addMassage(String.format("עין בכניסה למעטפה לא ראתה מסמך מגיע. (ברקוד : %s)", checkerCell.data));
            }
//            rotate array :
            Collections.rotate(cellsList, 1);
            initFirstCell();
//            check if no read event
            if (reader1WaitingForRead) {
                noReadEvent(reader1);
            }
            boolean reader2Active = gui.isReader2Active();
            if (reader2Active && reader2WaitingForRead) {
                noReadEvent(reader2);
            }
            gui.addNewEmptyRowToTable();
            reader1WaitingForRead = reader2WaitingForRead = false;
        }
        if (checkerEyeEventOnRising && checkerEye.rising || !checkerEyeEventOnRising && checkerEye.falling) { //INPUT 11
            Cell checkerCell = cellsList.get(checkerPosition);
            if (!checkerCell.exist) {
//                unexpected material arrived - error !
                stop();
                gui.addMassage("חומר לא צפוי בכניסה למעטפה ");
            } else {
                checkerCell.arrivedToChecker = true;
            }
        }
        if (diagnosticFrame != null)
        diagnosticFrame.updateInputsState(inputs);
    }

    private void initFirstCell() {
        Cell firstCell = cellsList.get(0);
        firstCell.data = null;
        firstCell.arrivedToChecker = false;
        firstCell.exist = false;
    }


    private void newTriggerArrived() {
//        will reset on read.
//        will check on cell shift event
        reader1WaitingForRead = reader2WaitingForRead = true;
//        send triggers
        try {
            reader1.writeBytes(PropertiesHandler.getProperty("triggerString", "<t>").getBytes());
            cellsList.get(materialFeederPosition).data = "waiting for read";
            gui.updateRow0("reader1", "waiting for read");
//            System.out.println("trigger");
            if (gui.isReader2Active())
                reader2.writeBytes(PropertiesHandler.getProperty("triggerString", "<t>").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void noReadEvent(Serial reader) {
        gui.updateRow0(reader.serialName, noReadString);
        if (reader.equals(reader1)) {
            if (++reader1NoReadCounter > maxNoReadAllowed) {
                stop();
                gui.addMassage("לא נקרא ברצף : " + reader1NoReadCounter + reader.serialName);

//                reader1NoReadCounter = 0;
            }
        } else if (reader.equals(reader2)) {
            if (++reader2NoReadCounter > maxNoReadAllowed) {
                stop();
                gui.addMassage("לא נקרא ברצף : " + reader2NoReadCounter + reader.serialName);
//                reader2NoReadCounter = 0;
            }
        }
    }

    private void newDataArrivedFromReader(String readerName, String data) {
        if (readerName.equals("reader1")) {
            if (reader1WaitingForRead) {
//                update cells list
                reader1WaitingForRead = false;
                cellsList.get(materialFeederPosition).data = data;
                if (data.equals(noReadString)) {
                    noReadEvent(reader1);
                } else {
//                valid data arrived
                    gui.updateRow0(readerName, data);
                    sequenceCheck(data, lastReader1, reader1NoReadCounter, ascending);
                    if (gui.isReader1FileCheckActive()) {
                        inFileCheck(data, barcodesFromFile);
                    }
                    if (gui.isReader2Active() && !reader2WaitingForRead)
                        matchCheck(data, reader2Current);
                    if (gui.isStartWithOne())
                        startWithOneCheck(data);
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
                } else {
//                    valid data arrived
                    gui.updateRow0(readerName, data);
                    if (!reader1WaitingForRead)
                        matchCheck(reader1current, data);
                    reader2Current = data;
                    reader2NoReadCounter = 0;
                }
            } else {
                unexpectedDataError(readerName, data);
            }
        }

    }

    private void startWithOneCheck(String data) {
        if (data.charAt(digitOnePositionInBarcode) != '1') {
            stop();
            gui.addMassage(String.format("%s ברקוד לא מתחיל בספרה 1", data));
            gui.addToErrorsSet(data);
            gui.refreshTableView();
        }
    }


    private void unexpectedDataError(String readerName, String data) {
        gui.addMassage(String.format("התקבלה קריאה (%s) מקורא: %s ללא שליחת טריגר ", data, readerName));
        gui.addToErrorsSet(data);
    }

    private void matchCheck(String reader1, String reader2) {
        if (!reader1.equals(reader2)) {
            stop();
            gui.addMassage(String.format("אין התאמה %s:%s", reader1, reader2));
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
                    gui.addMassage(String.format("חוסר רצף %s:%s (%s)", current, previous, directionText));
                    gui.refreshTableView();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void stop() {
//        send anything (between stx and etx) to stop io
        io.writeBytes(new byte[]{2, 99, 3});
    }

    private void inFileCheck(String barcode, Set<String> barcodes) {
        if (!barcodes.contains(barcode)) {
            gui.addMassage("ברקוד לא בקובץ: " + barcode);
            gui.addToErrorsSet(barcode);
            stop();
        }
    }

    private Set<String> getSetFromFile(String filePath) throws IOException {
        int startPos = Integer.parseInt(PropertiesHandler.getProperty("startPositionInLine_Include", "0"));
        int endPos = Integer.parseInt(PropertiesHandler.getProperty("endPositionInLine_Exclude", "8"));
        try (Stream<String> lines = Files.lines(Path.of(filePath))) {

            //            barcodes.forEach(System.out::println);
            return lines
                    .map(s -> s.substring(startPos, endPos))
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    Diag diagnosticFrame;

    public void setDiag(Diag diag) {
        diagnosticFrame = diag;
    }


//    private class Input {
//        boolean currentState, rising, falling;
//
//        public void update(boolean newState) {
//            if (!currentState && newState) {// rising
//                rising = true;
//                falling = false;
//            } else if (currentState && !newState) {//falling
//                rising = false;
//                falling = true;
//            } else {
//                rising = falling = false;
//            }
//            currentState = newState;
//        }
//
//    }

    private class Cell {
        boolean exist, arrivedToChecker;
        String data;
    }
}
