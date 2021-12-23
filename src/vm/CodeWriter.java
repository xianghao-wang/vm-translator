package vm;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

public class CodeWriter {
    /** Exception of code writer processing */
    public static class CodeWriterException extends RuntimeException {
        public CodeWriterException(String msg, Throwable e) {
            super(msg, e);
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
            put("stack", new Segment(256, 2047)); // SP
        }
    };

    private BufferedWriter fout; // for writing command line by line

    public CodeWriter(OutputStream out) {
        this.fout = new BufferedWriter(new OutputStreamWriter(out));

        // write base address of stack
        writeCommand("@" + VM_INIT.get("stack").begin);
        writeCommand("D=A");
        writeCommand("@SP");
        writeCommand("M=D");
    }

    /** Write command of one line */
    private void writeCommand(String command) {
        try {
            fout.write(command + "\n");
        } catch (Exception e) {
            throw new CodeWriterException("Cannot write into the output file.", e);
        }
    }

    /** Write arithmetic VM command into assembly */
    public void writeArithmetic(String command) {
        switch (command) {
            // unary operation
            case "neg", "not" -> {
                writeCommand("@SP");
                writeCommand("M=M-1");
                writeCommand("A=M");
                writeCommand("M=" + (command.equals("neg") ? "-M" : "!M"));
            }

            // binary operation
            default -> {
                // get the second operator
                writeCommand("@SP");
                writeCommand("M=M-1");
                writeCommand("A=M");
                writeCommand("D=M");

                // get the first calculator and do calculation
                writeCommand("@SP");
                writeCommand("M=M-1");
                writeCommand("A=M");
                switch (command) {
                    case "add" -> writeCommand("M=D+M");
                    case "sub" -> writeCommand("M=M-D");
                    // TODO eq, lt, gt
                    case "and" -> writeCommand("M=D&M");
                    case "or" -> writeCommand("M=D|M");
                }

                // store the result
                writeCommand("@SP");
                writeCommand("M=M+1");
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
                            default -> "";
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
                int exchange_reg = VM_INIT.get("general").begin;

                // store (segment + i) into exchange register
                switch (segment) {
                    case "argument", "local", "this", "that" -> {
                        writeCommand("@" + switch (segment) {
                            case "argument" -> "ARG";
                            case "local" -> "LCL";
                            case "this" -> "THIS";
                            case "that" -> "THAT";
                            default -> "";
                        });
                        writeCommand("D=M");
                        writeCommand("@" + index);
                        writeCommand("D=D+A");
                        writeCommand("@" + exchange_reg);
                        writeCommand("M=D");
                    }

                    case "static" -> {
                        writeCommand("@static." + index);
                        writeCommand("D=A");
                        writeCommand("@" + exchange_reg);
                        writeCommand("M=D");
                    }

                    case "temp" -> {
                        writeCommand("@" + (VM_INIT.get("temp").begin + index));
                        writeCommand("D=A");
                        writeCommand("@" + exchange_reg);
                        writeCommand("M=D");
                    }

                    case "pointer" -> {
                        writeCommand("@" + (index == 0 ? "THIS" : "THAT"));
                        writeCommand("D=A");
                        writeCommand("@" + exchange_reg);
                        writeCommand("M=D");
                    }
                }

                // decrement SP and pop the data in stack to D register
                writeCommand("@SP");
                writeCommand("M=M-1");
                writeCommand("A=M");
                writeCommand("D=M");

                // store data in D register into the memory of which address is stored in exchange register
                writeCommand("@" + exchange_reg);
                writeCommand("A=M");
                writeCommand("M=D");
            }
        }
    }

    public void close() {
        // infinite loop
        writeCommand("(program.end)");
        writeCommand("@program.end");
        writeCommand("0;JMP");

        try {
            fout.flush();
            fout.close();
        } catch (Exception e) {
            throw new CodeWriterException("Cannot close the output file.", e);
        }
    }
}
