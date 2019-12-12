import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class Day7 {
    public static void main(final String[] args) throws Exception {
        int currentHighscore = 0;

        final Stream<Stream<Integer>> permutationsStream = Permutations.of(Arrays.asList(5,6,7,8,9));
        final List<List<Integer>> permutations = permutationsStream.map(p -> p.collect(Collectors.toList())).collect(Collectors.toList());

        for (final List<Integer> parameters : permutations) {
            final ProgramIo[] ampsAndIo = new ProgramIo[]{
                new ProgramIo(parameters.get(0)),
                new ProgramIo(parameters.get(1)),
                new ProgramIo(parameters.get(2)),
                new ProgramIo(parameters.get(3)),
                new ProgramIo(parameters.get(4))
            };
            final int[][] instructions = new int[][]{
                getInstructions(getInput()),
                getInstructions(getInput()),
                getInstructions(getInput()),
                getInstructions(getInput()),
                getInstructions(getInput())
            };

            final CountDownLatch latch = new CountDownLatch(ampsAndIo.length);
            boolean firstRun = true;
            for (int i = 0; i < ampsAndIo.length; i++) {
                final ProgramIo ampIo = ampsAndIo[i];
                if (i == 0 && firstRun) {
                    // First run, let's give it a zero to kickstart it
                    ampIo.registerInput(0);
                } else if (i == 0) {
                    // First amp, get the last amps output
                    ampIo.registerInput(ampsAndIo[ampsAndIo.length - 1].getOutput());
                } else {
                    // Get the previous amps output
                    ampIo.registerInput(ampsAndIo[i - 1].getOutput());
                }

                if (firstRun) {
                    final int finalInt = i;
                    new Thread(() -> runProgram(instructions[finalInt], ampIo, latch)).start();
                }

                if (i == ampsAndIo.length - 1) {
                    final boolean latchDone = latch.await(10, TimeUnit.MILLISECONDS);

                    if (!latchDone) {
                        //We're not done yet!
                        //Reset i so loop can start over
                        firstRun = false;
                        i = -1;
                    } else {
                        final int output = ampIo.getOutput();
                        if (output > currentHighscore) {
                            currentHighscore = output;
                            System.out.println("CURRENT HIGHSCORE: " + currentHighscore);
                        }
                        break;
                    }
                }
            }
        }

        System.out.println("SUPER DUPER: " + currentHighscore);
    }

    private static void runProgram(
        final int[] instructions,
        final ProgramIo programIo,
        final CountDownLatch latch
    ) {
        int pos = 0;
        while (pos < instructions.length) {
            final int currentInstruction = instructions[pos];
            final String currentInstrStr = String.valueOf(currentInstruction);
            if (currentInstrStr.endsWith("1")) {
                pos += doAddition(currentInstrStr, instructions, pos);
            } else if (currentInstrStr.endsWith("2")) {
                pos += doMultiplication(currentInstrStr, instructions, pos);
            } else if (currentInstrStr.endsWith("3")) {
                pos += doInput(currentInstrStr, instructions, pos, programIo);
            } else if (currentInstrStr.endsWith("4")) {
                pos += doOutput(currentInstrStr, instructions, pos, programIo);
            } else if (currentInstrStr.endsWith("5")) {
                pos += jumpIfTrue(currentInstrStr, instructions, pos);
            } else if (currentInstrStr.endsWith("6")) {
                pos += jumpIfFalse(currentInstrStr, instructions, pos);
            } else if (currentInstrStr.endsWith("7")) {
                pos += ifLt(currentInstrStr, instructions, pos);
            } else if (currentInstrStr.endsWith("8")) {
                pos += ifEq(currentInstrStr, instructions, pos);
            } else if (currentInstrStr.endsWith("99")) {
                latch.countDown();
                break;
            } else {
                throw new IllegalArgumentException("Invalid opcode: " + currentInstrStr);
            }
        }
    }

    private static class ProgramIo {
        private final BlockingQueue<Integer> inputs = new ArrayBlockingQueue<>(10, true);
        private final BlockingQueue<Integer> outputs = new ArrayBlockingQueue<>(10, true);

        ProgramIo(final int... initialInputs) {
            for (final int initialInput : initialInputs) {
                registerInput(initialInput);
            }
        }

        int readInput() {
            try {
                return inputs.take();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        int getOutput() {
            try {
                return outputs.take();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        void registerOutput(final int output) {
            outputs.add(output);
            System.out.println("OUTPUT REGISTERED: " + output);
        }

        void registerInput(final int input) {
            inputs.add(input);
        }
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
        final int pos,
        final ProgramIo programIo
    ) {
        final int readVal = programIo.readInput();
        final int resultPos = instructions[pos + currentInstrStr.length()];
        instructions[resultPos] = readVal;

        return 2;
    }

    private static int doOutput(
        final String currentInstrStr,
        final int[] instructions,
        final int pos,
        final ProgramIo programIo
    ) {
        final Parameter[] parsedParams = parseParameters(currentInstrStr, instructions, pos, 1);
        final int result = parsedParams[0].readValue(instructions);
        programIo.registerOutput(result);

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

    private static int[] getInstructions(final String i) {
        return Arrays.stream(i.split(","))
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

    private static String getTestInputP1() {
        return "3,23,3,24,1002,24,10,24,1002,23,-1,23,"
            + "101,5,23,23,1,24,23,23,4,23,99,0,0";
    }

    private static String getTestInputP2() {
        return "3,52,1001,52,-5,52,3,53,1,52,56,54,1007,54,5,55,1005,55,26,1001,54,"
            + "-5,54,1105,1,12,1,53,54,53,1008,54,0,55,1001,55,1,55,2,53,55,53,4,"
            + "53,1001,56,-1,56,1005,56,6,99,0,0,0,0,10";
    }

    private static String getInput() {
        return "3,8,1001,8,10,8,105,1,0,0,21,34,59,76,101,114,195,276,357,438,99999,3,9,1001,9,4,9,1002,9,4,9,4,9,99,"
            + "3,9,102,4,9,9,101,2,9,9,102,4,9,9,1001,9,3,9,102,2,9,9,4,9,99,3,9,101,4,9,9,102,5,9,9,101,5,9,9,4,9,"
            + "99,3,9,102,2,9,9,1001,9,4,9,102,4,9,9,1001,9,4,9,1002,9,3,9,4,9,99,3,9,101,2,9,9,1002,9,3,9,4,9,99,3,"
            + "9,101,2,9,9,4,9,3,9,1002,9,2,9,4,9,3,9,1002,9,2,9,4,9,3,9,1001,9,1,9,4,9,3,9,102,2,9,9,4,9,3,9,101,1,"
            + "9,9,4,9,3,9,1001,9,1,9,4,9,3,9,1002,9,2,9,4,9,3,9,1001,9,2,9,4,9,3,9,102,2,9,9,4,9,99,3,9,101,2,9,9,4,"
            + "9,3,9,1002,9,2,9,4,9,3,9,1001,9,1,9,4,9,3,9,101,2,9,9,4,9,3,9,1002,9,2,9,4,9,3,9,102,2,9,9,4,9,3,9,"
            + "101,1,9,9,4,9,3,9,1001,9,2,9,4,9,3,9,101,2,9,9,4,9,3,9,1001,9,1,9,4,9,99,3,9,102,2,9,9,4,9,3,9,1001,9,"
            + "1,9,4,9,3,9,1001,9,1,9,4,9,3,9,102,2,9,9,4,9,3,9,1001,9,2,9,4,9,3,9,1002,9,2,9,4,9,3,9,102,2,9,9,4,9,"
            + "3,9,102,2,9,9,4,9,3,9,101,1,9,9,4,9,3,9,101,2,9,9,4,9,99,3,9,1002,9,2,9,4,9,3,9,102,2,9,9,4,9,3,9,102,"
            + "2,9,9,4,9,3,9,102,2,9,9,4,9,3,9,1002,9,2,9,4,9,3,9,1002,9,2,9,4,9,3,9,101,2,9,9,4,9,3,9,102,2,9,9,4,9,"
            + "3,9,101,2,9,9,4,9,3,9,101,2,9,9,4,9,99,3,9,1002,9,2,9,4,9,3,9,1001,9,1,9,4,9,3,9,101,2,9,9,4,9,3,9,"
            + "101,2,9,9,4,9,3,9,101,2,9,9,4,9,3,9,101,1,9,9,4,9,3,9,1002,9,2,9,4,9,3,9,1002,9,2,9,4,9,3,9,1001,9,1,"
            + "9,4,9,3,9,101,2,9,9,4,9,99";
    }

    public static class Permutations {

        public static <T> Stream<Stream<T>> of(final List<T> items) {
            return IntStream.range(0, factorial(items.size())).mapToObj(i -> permutation(i, items).stream());
        }

        private static int factorial(final int num) {
            return IntStream.rangeClosed(2, num).reduce(1, (x, y) -> x * y);
        }

        private static <T> List<T> permutation(final int count, final LinkedList<T> input, final List<T> output) {
            if (input.isEmpty()) { return output; }

            final int factorial = factorial(input.size() - 1);
            output.add(input.remove(count / factorial));
            return permutation(count % factorial, input, output);
        }

        private static <T> List<T> permutation(final int count, final List<T> items) {
            return permutation(count, new LinkedList<>(items), new ArrayList<>());
        }

    }
}
