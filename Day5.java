import java.util.Arrays;
import java.util.Scanner;

class Day5 {

    public static void main(final String[] args) {
        final int result = runProgram(getInstructions());
        System.out.println(result);
    }

    private static int runProgram(
        final int[] instructions,
        final int... args
    ) {
        for (int i = 0; i < args.length; i++) {
            instructions[i + 1] = args[i];
        }

        int pos = 0;
        while (pos < instructions.length) {
            final int currentInstruction = instructions[pos];
            final String currentInstrStr = String.valueOf(currentInstruction);
            if (currentInstrStr.endsWith("1")) {
                pos += doAddition(currentInstrStr, instructions, pos);
            } else if (currentInstrStr.endsWith("2")) {
                pos += doMultiplication(currentInstrStr, instructions, pos);
            } else if (currentInstrStr.endsWith("3")) {
                pos += doInput(currentInstrStr, instructions, pos);
            } else if (currentInstrStr.endsWith("4")) {
                pos += doOutput(currentInstrStr, instructions, pos);
            } else if (currentInstrStr.endsWith("5")) {
                pos += jumpIfTrue(currentInstrStr, instructions, pos);
            } else if (currentInstrStr.endsWith("6")) {
                pos += jumpIfFalse(currentInstrStr, instructions, pos);
            } else if (currentInstrStr.endsWith("7")) {
                pos += ifLt(currentInstrStr, instructions, pos);
            } else if (currentInstrStr.endsWith("8")) {
                pos += ifEq(currentInstrStr, instructions, pos);
            } else if (currentInstrStr.endsWith("99")) {
                break;
            } else {
                throw new IllegalArgumentException("Invalid opcode: " + currentInstrStr);
            }
        }

        System.out.println(Arrays.toString(instructions));
        return instructions[0];
    }

    private static int doAddition(
        final String currentInstr,
        final int[] instructions,
        final int currentPos
    ) {
        final int paramLength = 3;
        final Parameter[] parsedParams = parseParameters(currentInstr, instructions, currentPos, paramLength);

        final int val1 = parsedParams[0].readValue(instructions);
        final int val2 = parsedParams[1].readValue(instructions);
        final int resultPos = parsedParams[2].getRawValue();

        instructions[resultPos] = val1 + val2;
        return paramLength + 1;
    }

    private static int doMultiplication(
        final String currentInstr,
        final int[] instructions,
        final int currentPos
    ) {
        final int paramLength = 3;
        final Parameter[] parsedParams = parseParameters(currentInstr, instructions, currentPos, paramLength);

        final int val1 = parsedParams[0].readValue(instructions);
        final int val2 = parsedParams[1].readValue(instructions);
        final int resultPos = parsedParams[2].getRawValue();

        instructions[resultPos] = val1 * val2;
        return paramLength + 1;
    }

    private static int doInput(
        final String currentInstrStr,
        final int[] instructions,
        final int pos
    ) {
        final Scanner scanner = new Scanner(System.in);

        System.out.print("Enter input: ");
        final int readVal = Integer.parseInt(scanner.nextLine());
        final int resultPos = instructions[pos + currentInstrStr.length()];
        instructions[resultPos] = readVal;

        return 2;
    }

    private static int doOutput(
        final String currentInstrStr,
        final int[] instructions,
        final int pos
    ) {
        final Parameter[] parsedParams = parseParameters(currentInstrStr, instructions, pos, 1);
        final int result = parsedParams[0].readValue(instructions);
        System.out.println("OUTPUT: " + result);

        return 2;
    }

    private static int jumpIfTrue(final String currentInstrStr, final int[] instructions, final int pos) {
        final Parameter[] parsedParams = parseParameters(currentInstrStr, instructions, pos, 2);
        final boolean jump = parsedParams[0].readValue(instructions) > 0;
        if (jump) {
            return -pos + parsedParams[1].readValue(instructions);
        }
        return 3;
    }

    private static int jumpIfFalse(final String currentInstrStr, final int[] instructions, final int pos) {
        final Parameter[] parsedParams = parseParameters(currentInstrStr, instructions, pos, 2);
        final boolean jump = parsedParams[0].readValue(instructions) == 0;
        if (jump) {
            return -pos + parsedParams[1].readValue(instructions);
        }
        return 3;
    }

    private static int ifLt(final String currentInstrStr, final int[] instructions, final int pos) {
        final Parameter[] parsedParams = parseParameters(currentInstrStr, instructions, pos, 3);

        final int val1 = parsedParams[0].readValue(instructions);
        final int val2 = parsedParams[1].readValue(instructions);
        final int resultPos = parsedParams[2].getRawValue(); //???

        final int result = val1 < val2 ? 1 : 0;
        instructions[resultPos] = result;
        return 4;
    }

    private static int ifEq(final String currentInstrStr, final int[] instructions, final int pos) {
        final Parameter[] parsedParams = parseParameters(currentInstrStr, instructions, pos, 3);

        final int val1 = parsedParams[0].readValue(instructions);
        final int val2 = parsedParams[1].readValue(instructions);
        final int resultPos = parsedParams[2].getRawValue(); //???

        final int result = val1 == val2 ? 1 : 0;
        instructions[resultPos] = result;
        return 4;
    }

    private static Parameter[] parseParameters(
        final String currentInstr,
        final int[] instructions,
        final int currentPos,
        final int paramLength
    ) {
        final int[] paramModes = parseParameterModes(currentInstr, paramLength);
        final Parameter[] parsedParams = new Parameter[paramLength];

        for (int i = 0; i < paramLength; i++) {
            final int readFromPos = currentPos + 1 + i;
            final int paramMode = paramModes[i];
            parsedParams[i] = Parameter.of(paramMode, instructions[readFromPos]);
        }
        return parsedParams;
    }

    private static int[] parseParameterModes(final String currentInstr, final int paramLength) {
        final int[] paramModes = new int[paramLength];
        if (currentInstr.length() == 1) {
            //Single digit opcode, return an int array of zeroes (addr based modes)
            return paramModes;
        }
        // Remove opcode from instruction
        final char[] instrs = currentInstr.substring(0, currentInstr.length() - 2).toCharArray();
        for (int i = 0; i < instrs.length; i++) {
            // Read array backwards (right to left)
            final char paramMode = instrs[instrs.length - i - 1];
            // Put value from left to right in array (to match actual arg positions)
            paramModes[i] = Character.getNumericValue(paramMode);
        }
        return paramModes;
    }

    private static int[] getInstructions() {
        return Arrays.stream(getInput().split(","))
            .mapToInt(Integer::parseInt)
            .toArray();
    }

    private abstract static class Parameter {
        private final int value;

        private Parameter(final int value) {
            this.value = value;
        }

        private static Parameter of(final int mode, final int value) {
            if (mode == 0) {
                return new AddrPointer(value);
            }
            return new AbsoluteValue(value);
        }

        abstract int readValue(final int[] instructions);

        int getRawValue() {
            return this.value;
        }
    }

    private static class AddrPointer extends Parameter {
        private AddrPointer(final int address) {
            super(address);
        }

        @Override
        public int readValue(int[] instructions) {
            return instructions[getRawValue()];
        }
    }

    private static class AbsoluteValue extends Parameter {
        AbsoluteValue(final int value) {
            super(value);
        }

        @Override
        public int readValue(int[] instructions) {
            return getRawValue();
        }
    }

    private static String getInput() {
        return "3,225,1,225,6,6,1100,1,238,225,104,0,1102,45,16,225,2,65,191,224,1001,224,-3172,224,4,224,102,8,223,"
            + "223,1001,224,5,224,1,223,224,223,1102,90,55,225,101,77,143,224,101,-127,224,224,4,224,102,8,223,223,"
            + "1001,224,7,224,1,223,224,223,1102,52,6,225,1101,65,90,225,1102,75,58,225,1102,53,17,224,1001,224,-901,"
            + "224,4,224,1002,223,8,223,1001,224,3,224,1,224,223,223,1002,69,79,224,1001,224,-5135,224,4,224,1002,"
            + "223,8,223,1001,224,5,224,1,224,223,223,102,48,40,224,1001,224,-2640,224,4,224,102,8,223,223,1001,224,"
            + "1,224,1,224,223,223,1101,50,22,225,1001,218,29,224,101,-119,224,224,4,224,102,8,223,223,1001,224,2,"
            + "224,1,223,224,223,1101,48,19,224,1001,224,-67,224,4,224,102,8,223,223,1001,224,6,224,1,223,224,223,"
            + "1101,61,77,225,1,13,74,224,1001,224,-103,224,4,224,1002,223,8,223,101,3,224,224,1,224,223,223,1102,28,"
            + "90,225,4,223,99,0,0,0,677,0,0,0,0,0,0,0,0,0,0,0,1105,0,99999,1105,227,247,1105,1,99999,1005,227,99999,"
            + "1005,0,256,1105,1,99999,1106,227,99999,1106,0,265,1105,1,99999,1006,0,99999,1006,227,274,1105,1,99999,"
            + "1105,1,280,1105,1,99999,1,225,225,225,1101,294,0,0,105,1,0,1105,1,99999,1106,0,300,1105,1,99999,1,225,"
            + "225,225,1101,314,0,0,106,0,0,1105,1,99999,7,226,677,224,102,2,223,223,1005,224,329,1001,223,1,223,8,"
            + "226,677,224,1002,223,2,223,1005,224,344,101,1,223,223,8,226,226,224,1002,223,2,223,1006,224,359,101,1,"
            + "223,223,1008,677,226,224,1002,223,2,223,1005,224,374,1001,223,1,223,108,677,677,224,1002,223,2,223,"
            + "1005,224,389,1001,223,1,223,1107,226,677,224,1002,223,2,223,1006,224,404,101,1,223,223,1008,226,226,"
            + "224,102,2,223,223,1006,224,419,1001,223,1,223,7,677,226,224,1002,223,2,223,1005,224,434,101,1,223,223,"
            + "1108,226,226,224,1002,223,2,223,1005,224,449,101,1,223,223,7,226,226,224,102,2,223,223,1005,224,464,"
            + "101,1,223,223,108,677,226,224,102,2,223,223,1005,224,479,1001,223,1,223,1007,677,226,224,1002,223,2,"
            + "223,1006,224,494,1001,223,1,223,1007,677,677,224,1002,223,2,223,1006,224,509,1001,223,1,223,107,677,"
            + "677,224,1002,223,2,223,1005,224,524,101,1,223,223,1108,226,677,224,102,2,223,223,1006,224,539,1001,"
            + "223,1,223,8,677,226,224,102,2,223,223,1005,224,554,101,1,223,223,1007,226,226,224,102,2,223,223,1006,"
            + "224,569,1001,223,1,223,107,677,226,224,102,2,223,223,1005,224,584,1001,223,1,223,108,226,226,224,102,"
            + "2,223,223,1006,224,599,1001,223,1,223,107,226,226,224,1002,223,2,223,1006,224,614,1001,223,1,223,1108,"
            + "677,226,224,1002,223,2,223,1005,224,629,1001,223,1,223,1107,677,677,224,102,2,223,223,1005,224,644,"
            + "1001,223,1,223,1008,677,677,224,102,2,223,223,1005,224,659,101,1,223,223,1107,677,226,224,1002,223,2,"
            + "223,1006,224,674,101,1,223,223,4,223,99,226";
    }
}
