import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class Day7 {

    public static void main(final String[] args) throws Exception {
        final Stream<Stream<Integer>> permutationsStream = Permutations.of(Arrays.asList(9, 7, 8, 5, 6));
        final List<List<Integer>> permutations = permutationsStream.map(p -> p.collect(toList())).collect(toList());

        int currentHighscore = 0;

        for (final List<Integer> parameters : permutations) {
            final List<IntComp> amplifiers = List.of(
                new IntComp(getInstructions(getInput())),
                new IntComp(getInstructions(getInput())),
                new IntComp(getInstructions(getInput())),
                new IntComp(getInstructions(getInput())),
                new IntComp(getInstructions(getInput()))
            );

            amplifiers.get(0).addInput(parameters.get(0));
            amplifiers.get(1).addInput(parameters.get(1));
            amplifiers.get(2).addInput(parameters.get(2));
            amplifiers.get(3).addInput(parameters.get(3));
            amplifiers.get(4).addInput(parameters.get(4));

            for (int i = 0; i < amplifiers.size(); i++) {
                final int previousIdx = i == 0 ? amplifiers.size() - 1 : i - 1;
                final IntComp amplifier = amplifiers.get(i);
                final IntComp previousAmp = amplifiers.get(previousIdx);

                final int previousAmpOutput = coalesce(previousAmp.output.poll());
                amplifier.addInput(previousAmpOutput);

                while (amplifier.hasMoreInstructions()) {
                    amplifier.step();
                }

                if (i == amplifiers.size() - 1) {
                    // Last amp
                    final Integer output = amplifier.output.peek();
                    System.out.println("AMP E OUTPUT: " + output);
                    // Did it halt? eg not waiting for Amp D output? If so, we're done with this permutation entry
                    if (amplifier.terminated) {
                        System.out.println("TERMINATED! Run next itr");
                        if (output > currentHighscore) {
                            currentHighscore = output;
                        }
                    } else {
                        // Amp is waiting for more input, as it's halted but not terminated. Loop again
                        i = -1; // Restart loop
                    }
                }
            }
        }

        System.out.println();
        System.out.println("WINNING HIGHSCORE: " + currentHighscore);
    }

    private static int coalesce(final Integer v) {
        return v == null ? 0 : v;
    }

    private static class IntComp {
        private int pos = 0;
        private int rbase = 0;
        private boolean halted = false;
        private boolean terminated = false;
        private final int[] instructions;
        private final Queue<Integer> input = new ArrayBlockingQueue<>(10);
        private final Queue<Integer> output = new ArrayBlockingQueue<>(10);

        private IntComp(final int[] instructions) {
            this.instructions = instructions;
        }

        public boolean hasMoreInstructions() {
            return pos < instructions.length && !halted && !terminated;
        }

        public void step() throws Exception {
            final int instr = instructions[pos];
            final Op opCode = Op.of(instr % 100);
            final int[] parameters = Arrays.copyOfRange(instructions, pos + 1, pos + 1 + opCode.parameters);
            final int[] paramModes = parseParamModes(instr, opCode.parameters);

            doOp(opCode, parameters, paramModes);
        }

        void addInput(final int inp) {
            this.input.add(inp);
            this.halted = false;
        }

        private void doOp(
            final Op opCode,
            final int[] params,
            final int[] modes
        ) throws Exception {
            if (Op.ADD == opCode) {
                final int result = read(params[0], modes[0]) + read(params[1], modes[1]);
                write(params[2], modes[2], result);
            } else if (Op.MUL == opCode) {
                final int result = read(params[0], modes[0]) * read(params[1], modes[1]);
                write(params[2], modes[2], result);
            } else if (Op.INPUT == opCode) {
                final Integer input = this.input.poll();
                if (input == null) {
                    halted = true;
                } else {
                    write(params[0], modes[0], input);
                }
            } else if (Op.OUTPUT == opCode) {
                final Integer output = read(params[0], modes[0]);
                this.output.add(output);
            } else if (Op.JIT == opCode) {
                final boolean jump = read(params[0], modes[0]) > 0;
                if (jump) {
                    pos = read(params[1], modes[1]) - params.length - 1;
                }
            } else if (Op.JIF == opCode) {
                final boolean jump = read(params[0], modes[0]) == 0;
                if (jump) {
                    pos = read(params[1], modes[1]) - params.length - 1;
                }
            } else if (Op.LT == opCode) {
                final int bit = read(params[0], modes[0]) < read(params[1], modes[1]) ? 1 : 0;
                write(params[2], modes[2], bit);
            } else if (Op.EQ == opCode) {
                final int bit = read(params[0], modes[0]) == read(params[1], modes[1]) ? 1 : 0;
                write(params[2], modes[2], bit);
            } else if (Op.HALT == opCode) {
                halted = true;
                terminated = true;
            }

            if (!halted) {
                pos += params.length + 1;
            }
        }

        private int[] parseParamModes(final int instr, final int parameters) {
            final int[] modes = new int[parameters];
            final int instrParams = instr / 100;
            final char[] instrC = String.valueOf(instrParams).toCharArray();

            for (int i = 0; i < parameters; i++) {
                final int rev = instrC.length - i - 1;
                if (rev >= 0) {
                    modes[i] = Character.getNumericValue(instrC[rev]);
                } else {
                    modes[i] = 0;
                }
            }
            return modes;
        }

        int read(final int pos, final int mode) {
            switch (mode) {
                case 0:
                    return instructions[pos];
                case 1:
                    return pos;
                case 2:
                    return instructions[pos + rbase];
            }
            throw new IllegalArgumentException("Unknown mode: " + mode);
        }

        void write(final int pos, final int mode, final int inp) {
            instructions[pos] = inp;
        }
    }

    private enum Op {
        ADD(1, 3),
        MUL(2, 3),
        INPUT(3, 1),
        OUTPUT(4, 1),
        JIT(5, 2),
        JIF(6, 2),
        LT(7, 3),
        EQ(8, 3),
        RBASE(9, 1),
        HALT(99, 0);

        private final int opCode;
        private final int parameters;

        Op(final int opCode, final int parameters) {
            this.opCode = opCode;
            this.parameters = parameters;
        }

        static Op of(final int opCode) {
            return Stream.of(values()).filter(o -> o.opCode == opCode)
                .findFirst()
                .orElse(HALT);
        }
    }

    private static int[] getInstructions(final String input) {
        return Arrays.stream(input.split(","))
            .mapToInt(Integer::parseInt)
            .toArray();
    }

    private static String getTestInputP1() {
        return "3,31,3,32,1002,32,10,32,1001,31,-2,31,1007,31,0,33,"
            + "1002,33,7,33,1,33,31,31,1,32,31,31,4,31,99,0,0,0";
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
