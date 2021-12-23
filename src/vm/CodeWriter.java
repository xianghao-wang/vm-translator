package vm;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

public class CodeWriter {
    /** Exception of code writer processing */
    public static class CodeWriterException extends RuntimeException {
        public CodeWriterException(int lineNumber, String msg) {
            super(String.format("Line %d: %s", lineNumber, msg));
        }
    }

    /** Represents a memory segment */
    private static class Segment {
        final int begin; // inclusive begin
        final int end; // inclusive end
        public Segment(int begin, int end) {
            this.begin = begin;
            this.end = end;
        }

    }

    private static Map<String, Segment> VM_INIT = new HashMap<>() {
        {
            put("pointer", new Segment(3, 4));
            put("temp", new Segment(5, 12));
            put("general", new Segment(13, 15));
            put("static", new Segment(16, 255));
            put("stack", new Segment(256, 2047));

            put("local", new Segment(2048, 2303));
            put("argument", new Segment(2304, 2559));
            put("this", new Segment(2560, 2815));
            put("that", new Segment(2816, 3070));
        }
    };

    private BufferedWriter fout; // for writing command line by line

    public CodeWriter(OutputStream out) {
        BufferedWriter fout = new BufferedWriter(new OutputStreamWriter(out));

        // write the init of vm
        allocateValue("SP", VM_INIT.get("stack").begin);
        allocateValue("LCL", VM_INIT.get("local").begin);
        allocateValue("ARG", VM_INIT.get("argument").begin);
        allocateValue("THIS", VM_INIT.get("this").begin);
        allocateValue("THAT", VM_INIT.get("that").begin);
    }

    /** Write command of one line */
    private void writeCommand(String command) {
        try {
            fout.write(command + "\n");
        } catch (Exception e) {
            throw new CodeWriterException(1, "cannot write into the output file.");
        }
    }

    /** Allocate value to a memory represented by the symbol */
    private void allocateValue(String symbol, int value) {
        writeCommand("@" + symbol);
        writeCommand("M=" + value);
    }

    /** Write arithmetic VM command into assembly */
    public void writeArithmetic(String command) {
        switch (command) {
            // unary operation
            case "neg", "not" -> {
                writeCommand("@SP");
                writeCommand("A=M");
                writeCommand("M=" + (command.equals("neg") ? "-M" : "!M"));
                writeCommand("@SP");
                writeCommand("M=M-1");
            }

            // binary operation
            default -> {
                // get the second operator
                writeCommand("@SP");
                writeCommand("A=M");
                writeCommand("D=M");
                writeCommand("@SP");
                writeCommand("M=M-1");

                // calculation
                writeCommand("A=M");
                switch (command) {
                    case "add" -> writeCommand("M=D+M");
                    case "sub" -> writeCommand("M=M-D");
                    // TODO eq, lt, gt
                    case "and" -> writeCommand("M=D&M");
                    case "or" -> writeCommand("M=D|M");
                }
            }
        }
    }

    /** Write push/pop command into assembly */
    public void writePushPop(Parser.CommandType commandType, String segment, int index) {
        switch (commandType) {
            // push command
            case C_PUSH -> {
                // get segment i
                switch (segment) {
                    case "argument", "local", "this", "that" -> {
                        writeCommand("@" + switch (segment) {
                            case "argument" -> "ARG";
                            case "local" -> "LCL";
                            case "this" -> "THIS";
                            case "that" -> "THAT";
                        });
                        writeCommand("D=M");
                        writeCommand("@" + index);
                        writeCommand("A=D+A");
                        writeCommand("D=M");
                    }

                    case "static" -> {
                        writeCommand("@static." + index);
                        writeCommand("D=M");
                    }

                    case "constant" -> {
                        writeCommand("@" + index);
                        writeCommand("D=A");
                    }

                    case "temp" -> {
                        writeCommand("@" + (VM_INIT.get("temp").begin + index));
                        writeCommand("D=M");
                    }

                    case "pointer" -> {
                        writeCommand("@" + (index == 0 ? "THIS" : "THAT"));
                        writeCommand("D=M");
                    }
                }

                // push data to stack
                writeCommand("@SP");
                writeCommand("A=M");
                writeCommand("M=D");
                writeCommand("@SP");
                writeCommand("M=M+1");
            }

            // pop command
            case C_POP -> {

            }
        }
    }
}