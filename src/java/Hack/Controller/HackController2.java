package Hack.Controller;

import javax.swing.*;
import java.awt.event.*;
import java.io.*;
import java.util.Vector;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import Hack.Utilities.*;
import Hack.Events.*;
import Hack.Gates.HDLException;

public class HackController2
 implements ControllerEventListener, ActionListener, ProgramEventListener {

    /**
     * The number of speed units.
     */
    //public static final int NUMBER_OF_SPEED_UNITS = 5;

    /**
     * The speed function for data flow animation.
     */
    //public static final float[] SPEED_FUNCTION = {0f, 0.35f, 0.63f, 0.87f, 1f};

    /**
     * The speed function for fast forward mode.
     */
    //public static final int[] FASTFORWARD_SPEED_FUNCTION = {500, 1000, 2000, 4000, 15000};

    // ANIMATION MODES:

    /**
     * Animation mode: Specifies using static display changes - displays value changes staticaly
     */
    //public static final int DISPLAY_CHANGES = 0;

    /**
     * Animation mode: Specifies using dynamic animation - fully animates value changes
     */
    //public static final int ANIMATION = 1;

    /**
     * Animation mode: Specifies using no display changes.
     * In this mode, the speed has no meening.
     */
    //public static final int NO_DISPLAY_CHANGES = 2;


    // NUMERIC FORMATS:

    /**
     * Decimal numeric format
     */
    public static final int DECIMAL_FORMAT = 0;

    /**
     * Hexadecimal numeric format
     */
    public static final int HEXA_FORMAT = 1;

    /**
     * Binary numeric format
     */
    public static final int BINARY_FORMAT = 2;


    // ADDITIONAL DISPLAYS

    /**
     * Specifies the additional display of the script file component.
     */
    //public static final int SCRIPT_ADDITIONAL_DISPLAY = 0;

    /**
     * Specifies the additional display of the output file component.
     */
    //public static final int OUTPUT_ADDITIONAL_DISPLAY = 1;

    /**
     * Specifies the additional display of the comparison file component.
     */
    //public static final int COMPARISON_ADDITIONAL_DISPLAY = 2;

    /**
     * Specifies no additional display.
     */
    //public static final int NO_ADDITIONAL_DISPLAY = 3;

    // The default dir for loading script files
    private static final String INITIAL_SCRIPT_DIR = "scripts";

    // Minimum and maximum mili-seconds per script command execution
    private static final int MAX_MS = 2500;
    private static final int MIN_MS = 25;

    // Initial speed unit
    private static final int INITIAL_SPEED_UNIT = 3;

    // A helper string with spaces
    private static final String SPACES = "                                        ";

    // The contorller's GUI
    //protected ControllerGUI gui;

    // The file of the current script
    private File currentScriptFile;

    // The names of the output and comparison files
    private String currentOutputName;
    private String currentComparisonName;

    // The script commands
    private Script script;

    // The controlled simulator
    protected HackSimulator simulator;

    // The current speed unit.
    private int currentSpeedUnit;

    // The current animation mode.
    private int animationMode;

    // The program counter
    private int currentCommandIndex;

    // The output desination
    private PrintWriter output;

    // The comparison source
    private BufferedReader comparisonFile;

    // Index of repeat or while start command
    private int loopCommandIndex;

    // Number of repeats left
    private int repeatCounter;

    // The condition of the current while loop.
    private ScriptCondition whileCondititon;

    // The current variable printing list
    private VariableFormat[] varList;

    // The current compared and output lines
    private int compareLinesCounter, outputLinesCounter;

    // times the fast forward process
    private Timer timer;

    // locked when single step in process
    protected boolean singleStepLocked;


    // True if the system is in fast forward.
    private boolean fastForwardRunning;

    // True if the system is in Single Step.
    private boolean singleStepRunning;

    // True if the script ended.
    private boolean scriptEnded;
    public boolean getScriptEnded(){ return scriptEnded; }

    // True if the program was halted.
    private boolean programHalted;
    public boolean getProgramHalted(){ return programHalted; }

    // The speed delays.
    private int[] delays;

    // true if the comparison failed at some point in the script
    private boolean comparisonFailed;
    public boolean getComparisonFailed(){ return comparisonFailed; }

    // The number of the line in which the comparison failed (if it failed).
    private int comparisonFailureLine;
    public int getComparisonFailureLine(){ return comparisonFailureLine; }

    public boolean isSuccess(){
        return scriptEnded && !programHalted && !comparisonFailed;
    }

    // The echo that was displayed (if any) when single step was stopped in the middle.
    private String lastEcho;


    /**
     * Constructs a new HackController with the given script file name.
     * The script will be executed and the final result will be printed.
     */
    public HackController2(HackSimulator simulator, String scriptFileName) {
        File file = new File(scriptFileName);
        if (!file.exists())
            displayMessage(scriptFileName + " doesn't exist", true);

        this.simulator = simulator;
        simulator.addListener(this);

        try {
            loadNewScript(file, false);
        } catch (ScriptException se) {
            displayMessage(se.getMessage(), true);
        } catch (ControllerException ce) {
            displayMessage(ce.getMessage(), true);
        }
    }

    public boolean runScript(){
        rewind();
        fastForwardRunning = true;
        try{
        while (fastForwardRunning)
             singleStep();
        }catch(Exception ex){
            programHalted = true;
            return false;
        }

        return isSuccess();
    }

    // Restarts the current script from the beginning.
    private void rewind() {
        try {
            scriptEnded = false;
            programHalted = false;

            simulator.restart();

            if (output != null)
                resetOutputFile();
            if (comparisonFile != null)
                resetComparisonFile();

            lastEcho = "";
            currentCommandIndex = 0;
        } catch (ControllerException e) {
            displayMessage(e.getMessage(), true);
        }
    }

    // Puts the controller into stop mode
    private void stopMode() {
        if (fastForwardRunning) {
            fastForwardRunning = false;
        }
        singleStepRunning = false;
    }

    // Executes a single step from the script, checks for a breakpoint and
    // sets the status of the system accordingly.
    private synchronized void singleStep() {

        singleStepLocked = true;

        try {
            byte terminatorType;
            singleStepRunning = true;

            do {
                terminatorType = miniStep();
            } while (terminatorType == Command.MINI_STEP_TERMINATOR && singleStepRunning);

            singleStepRunning = false;

            if (terminatorType == Command.STOP_TERMINATOR) {
                displayMessage("Script reached a '!' terminator", false);
                stopMode();
            }
        } catch (ControllerException ce) {
            stopWithError(ce);
        } catch (ProgramException pe) {
            stopWithError(pe);
        } catch (CommandException ce) {
            stopWithError(ce);
        } catch (VariableException ve) {
            stopWithError(ve);
        }

        singleStepLocked = false;
        notifyAll();
    }

    // Displays the message of the given exception and stops the script's execution.
    private void stopWithError(Exception e) {
        displayMessage(e.getMessage(), true);
        stopMode();
    }

    // Executes one command from the script and advances to the next.
    // Returns the command's terminator.
    private byte miniStep()
     throws ControllerException, ProgramException, CommandException, VariableException {
        Command command;
        boolean redo;

        do {
            command = script.getCommandAt(currentCommandIndex);
            redo = false;

            switch (command.getCode()) {
            case Command.SIMULATOR_COMMAND:
                simulator.doCommand((String[])command.getArg());
                break;
            case Command.OUTPUT_FILE_COMMAND:
                doOutputFileCommand(command);
                break;
            case Command.COMPARE_TO_COMMAND:
                doCompareToCommand(command);
                break;
            case Command.OUTPUT_LIST_COMMAND:
                doOutputListCommand(command);
                break;
            case Command.OUTPUT_COMMAND:
                doOutputCommand(command);
                break;
            case Command.ECHO_COMMAND:
                doEchoCommand(command);
                break;
            case Command.CLEAR_ECHO_COMMAND:
                doClearEchoCommand(command);
                break;
            case Command.BREAKPOINT_COMMAND:
                break;
            case Command.CLEAR_BREAKPOINTS_COMMAND:
                break;
            case Command.REPEAT_COMMAND:
                repeatCounter = ((Integer)command.getArg()).intValue();
                loopCommandIndex = currentCommandIndex + 1;
                redo = true;
                break;
            case Command.WHILE_COMMAND:
                whileCondititon = (ScriptCondition)command.getArg();
                loopCommandIndex = currentCommandIndex + 1;
                if (!whileCondititon.compare(simulator)) {
                    // advance till the nearest end while command.
                    for (; script.getCommandAt(currentCommandIndex).getCode() !=
                           Command.END_WHILE_COMMAND; currentCommandIndex++);
        }
                redo = true; // whether the test was successful or not,
               // the while command doesn't count
                break;
            case Command.END_SCRIPT_COMMAND:
                scriptEnded = true;
                stopMode();

                try {
                    if (output != null)
                        output.close();

                    if (comparisonFile != null) {
                        if (comparisonFailed)
                            displayMessage("End of script - Comparison failure at line "
                                               + comparisonFailureLine, true);
                        else
                            displayMessage("End of script - Comparison ended successfully",
                                               false);

                        comparisonFile.close();
                    }
                    else
                        displayMessage("End of script " + currentScriptFile.getName(), false);
                } catch (IOException ioe) {
                    throw new ControllerException("Could not read comparison file");
                }

                break;
            }

            // advance script line pointer
            if (command.getCode() != Command.END_SCRIPT_COMMAND) {
                currentCommandIndex++;
                Command nextCommand = script.getCommandAt(currentCommandIndex);
                if (nextCommand.getCode() == Command.END_REPEAT_COMMAND) {
                    if (repeatCounter == 0 || --repeatCounter > 0)
                        currentCommandIndex = loopCommandIndex;
                    else
                        currentCommandIndex++;
                }
                else if (nextCommand.getCode() == Command.END_WHILE_COMMAND) {
                    if (whileCondititon.compare(simulator))
                        currentCommandIndex = loopCommandIndex;
                    else
                        currentCommandIndex++;
                }
            }

        } while (redo);

        return command.getTerminator();
    }

    // Executes the controller's output-file command.
    private void doOutputFileCommand(Command command) throws ControllerException {
        currentOutputName = currentScriptFile.getParent() + "/" + (String)command.getArg();
        resetOutputFile();
    }

    // Executes the controller's compare-to command.
    private void doCompareToCommand(Command command) throws ControllerException {
        currentComparisonName = currentScriptFile.getParent() + "/" + (String)command.getArg();
        resetComparisonFile();
    }

    // Executes the controller's output-list command.
    private void doOutputListCommand(Command command) throws ControllerException {
        if (output == null)
            throw new ControllerException("No output file specified");

        varList = (VariableFormat[])command.getArg();
        StringBuffer line = new StringBuffer("|");

        for (int i = 0; i < varList.length; i++) {
            int space = varList[i].padL + varList[i].padR + varList[i].len;
            String varName = varList[i].varName.length() > space ?
                             varList[i].varName.substring(0, space) : varList[i].varName;
            int leftSpace = (int)((space - varName.length()) / 2);
            int rightSpace = space - leftSpace - varName.length();

            line.append(SPACES.substring(0, leftSpace) + varName +
                        SPACES.substring(0, rightSpace) + '|');
        }

        outputAndCompare(line.toString());
    }

    // Executes the controller's output command.
    private void doOutputCommand(Command command) throws ControllerException, VariableException {
        if (output == null)
            throw new ControllerException("No output file specified");

        StringBuffer line = new StringBuffer("|");

        for (int i = 0; i < varList.length; i++) {
            // find value string (convert to require format if necessary)
            String value = simulator.getValue(varList[i].varName);
            if (varList[i].format != VariableFormat.STRING_FORMAT) {
                int numValue;
                try {
                    numValue = Integer.parseInt(value);
                } catch (NumberFormatException nfe) {
                    throw new VariableException("Variable is not numeric", varList[i].varName);
                }
                if (varList[i].format == VariableFormat.HEX_FORMAT)
                    value = Conversions.decimalToHex(numValue, 4);
                else if (varList[i].format == VariableFormat.BINARY_FORMAT)
                    value = Conversions.decimalToBinary(numValue, 16);
            }

            if (value.length() > varList[i].len)
                value = value.substring(value.length() - varList[i].len);

            int leftSpace = varList[i].padL +
                            (varList[i].format == VariableFormat.STRING_FORMAT ?
                             0 : (varList[i].len - value.length()));
            int rightSpace = varList[i].padR +
                            (varList[i].format == VariableFormat.STRING_FORMAT ?
                             (varList[i].len - value.length()) : 0);
            line.append(SPACES.substring(0, leftSpace) + value +
                        SPACES.substring(0, rightSpace) + '|');
        }

        outputAndCompare(line.toString());
    }

    // Executes the controller's echo command.
    private void doEchoCommand(Command command) throws ControllerException {
        lastEcho = (String)command.getArg();
    }

    // Executes the controller's Clear-echo command.
    private void doClearEchoCommand(Command command) throws ControllerException {
        lastEcho = "";
    }

    // Compares an output line with a template line from a compare file.
    // The template must match exactly except for '*' which may match any
    // single character.
    private static boolean compareLineWithTemplate(String out, String cmp) {
        if (out.length() != cmp.length()) {
            return false;
        }
        StringCharacterIterator outi = new StringCharacterIterator(out);
        StringCharacterIterator cmpi = new StringCharacterIterator(cmp);
        for (outi.first(), cmpi.first();
             outi.current() != CharacterIterator.DONE;
             outi.next(), cmpi.next()) {
            if (cmpi.current() != '*' && outi.current() != cmpi.current()) {
                return false;
            }
        }
        return true;
    }

    // Ouputs the given line into the output file and compares it to the current
    // compare file (if exists)
    private void outputAndCompare(String line) throws ControllerException {
        output.println(line);
        output.flush();

        outputLinesCounter++;

        if (comparisonFile != null) {
            try {
                String compareLine = comparisonFile.readLine();

                compareLinesCounter++;

                if (!compareLineWithTemplate(line, compareLine)) {
                    comparisonFailed = true;
                    comparisonFailureLine = compareLinesCounter;
                    displayMessage("Comparison failure at line " + comparisonFailureLine,
                                       true);
                    stopMode();
                }
            } catch (IOException ioe) {
                throw new ControllerException("Could not read comparison file");
            }
        }
    }

    // loads the given script file and restarts the GUI.
    protected void loadNewScript(File file, boolean displayMessage)
     throws ControllerException, ScriptException {
        currentScriptFile = file;
        script = new Script(file.getPath());
        
        currentCommandIndex = 0;
        output = null;
        currentOutputName = "";
        comparisonFile = null;
        currentComparisonName = "";

        if (displayMessage)
            displayMessage("New script loaded: " + file.getPath(), false);
    }

    // Resets the output file.
    private void resetOutputFile() throws ControllerException {
        try {
            output = new PrintWriter(new FileWriter(currentOutputName));
            outputLinesCounter = 0;
        } catch (IOException ioe) {
            throw new ControllerException("Could not create output file " + currentOutputName);
        }
    }

    // Resets the comparison file.
    private void resetComparisonFile() throws ControllerException {
        try {
            comparisonFile = new BufferedReader(new FileReader(currentComparisonName));
            compareLinesCounter = 0;
            comparisonFailed = false;
        } catch (IOException ioe) {
            throw new ControllerException("Could not open comparison file " +
                                          currentComparisonName);
        }
    }

    // Displays the given message with the given type (error or not)
    private void displayMessage(String message, boolean error) {
        message = currentScriptFile.getName() + ": " + message;
        if (error) {
            System.err.println(message);
            System.err.flush(); // Flush to prevent intermixing of err and out stream
        }
        else {
            System.out.println(message);
            System.out.flush(); // Flush to prevent intermixing of err and out stream
        }
    }

    // Returns the version string
    private static String getVersionString() {
        return " (" + Definitions.version + ")";
    }

    public void actionPerformed(ActionEvent e) {
    }

    public void programChanged(ProgramEvent event) {
        switch (event.getType()) {
            case ProgramEvent.SAVE:
                break;
            case ProgramEvent.LOAD:
                break;
            case ProgramEvent.CLEAR:
                break;
        }
    }

    public void actionPerformed(ControllerEvent event) {
        try {
            switch (event.getAction()) {
                case ControllerEvent.SINGLE_STEP:
                    break;
                case ControllerEvent.FAST_FORWARD:
                    break;
                case ControllerEvent.STOP:
                    stopMode();
                    break;
                case ControllerEvent.REWIND:
                    displayMessage("Script restarted", false);
                    rewind();
                    break;
                case ControllerEvent.SPEED_CHANGE:
                    break;
                case ControllerEvent.BREAKPOINTS_CHANGE:
                    break;
                case ControllerEvent.SCRIPT_CHANGE:
                    File file = (File)event.getData();
                    loadNewScript(file, true);
                    rewind();
                    break;
                case ControllerEvent.ANIMATION_MODE_CHANGE:
                    break;
                case ControllerEvent.NUMERIC_FORMAT_CHANGE:
                    break;
                case ControllerEvent.ADDITIONAL_DISPLAY_CHANGE:
                    break;
                case ControllerEvent.DISABLE_ANIMATION_MODE_CHANGE:
                    break;
                case ControllerEvent.ENABLE_ANIMATION_MODE_CHANGE:
                    break;
                case ControllerEvent.DISABLE_SINGLE_STEP:
                    break;
                case ControllerEvent.ENABLE_SINGLE_STEP:
                    break;
                case ControllerEvent.DISABLE_FAST_FORWARD:
                    break;
                case ControllerEvent.ENABLE_FAST_FORWARD:
                    break;
                case ControllerEvent.LOAD_PROGRAM:
                    simulator.loadProgram();
                    break;
                case ControllerEvent.HALT_PROGRAM:
                    displayMessage("End of program", false);
                    programHalted = true;
                    if (fastForwardRunning)
                        stopMode();
                    break;
                case ControllerEvent.CONTINUE_PROGRAM:
                    if (programHalted) {
                        programHalted = false;
                    }
                    break;
                case ControllerEvent.DISABLE_MOVEMENT:
                    break;
                case ControllerEvent.ENABLE_MOVEMENT:
                    break;
                case ControllerEvent.DISPLAY_MESSAGE:
                    displayMessage((String)event.getData(), false);
                    break;
                case ControllerEvent.DISPLAY_ERROR_MESSAGE:
                    if (timer.isRunning())
                        stopMode();
                    displayMessage((String)event.getData(), true);
                    break;
                default:
                    doUnknownAction(event.getAction(), event.getData());
                    break;
            }
        } catch (ScriptException e) {
            displayMessage(e.getMessage(), true);
            stopMode();
        } catch (ControllerException e) {
            displayMessage(e.getMessage(), true);
            stopMode();
        }
    }

    /**
     * Executes an unknown controller action event.
     */
    protected void doUnknownAction(byte action, Object data) throws ControllerException {
    }
}
