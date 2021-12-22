package vm;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Parser {
    /** All command types */
    public enum CommandType {
        C_ARITHMETIC,
        C_PUSH, C_POP,
        C_LABEL, C_GOTO,
        C_IF, C_FUNCTION,
        C_RETURN, C_CALL
    }

    /** Exception of parser processing */
    public static class ParseException extends Exception {
        public ParseException(int lineNumber, String msg) {
            super(String.format("Line %d: %s", lineNumber, msg));
        }
    }

    private List<String> lines;
    private int idx;

    public Parser(InputStream in) {
        Scanner scanner = new Scanner(in);

        this.idx = -1;
        // read all lines to a list
        this.lines = new ArrayList<>();
        while (scanner.hasNextLine()) {
            lines.add(scanner.nextLine().strip());
        }
    }

    /** Check if there are more commands to parse */
    public boolean hasMoreCommands() {
        return this.idx != lines.size() - 1;
    }

    /** Advanced to the next command */
    public void advance() {
        this.idx += 1;
    }

    /** Return the type of current command */
    public CommandType commandType() throws ParseException {
        String[] pieces = command().split(" ");

        switch (pieces[0]) {
            // arithmetic & logical command
            case "add", "sub", "neg", "eq", "gt", "lt", "and", "or", "not" -> {
                if (pieces.length == 1) {
                    return CommandType.C_ARITHMETIC;
                }
            }

            // push command
            case "push", "pop" -> {
                if (pieces.length == 3) {
                    return pieces[0].equals("push") ? CommandType.C_PUSH : CommandType.C_POP;
                }
            }

            // TODO check the braching and function command type
        }

        throw new ParseException(lineNumber(), String.format("unrecgonized command: \"%s\"", command()));


    }

    /** Return the current command */
    private String command() {
        return this.lines.get(idx);
    }

    /** Return the current line number */
    private int lineNumber() {
        return idx;
    }
}
