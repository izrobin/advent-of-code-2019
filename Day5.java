import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Stream;

class Day5 {

    public static void main(final String[] args) throws Exception {
        final int[] input = getInstructions(getInput());
        final IntComp comp = new IntComp(input);

        comp.input.add(5);
        
        while (comp.hasMoreInstructions()) {
            comp.step();
        }

        System.out.println(comp.output);
    }

    private static class IntComp {
        private int pos = 0;
        private int rbase = 0;
        private boolean halted = false;
        private final int[] instructions;
        private final BlockingQueue<Integer> input = new ArrayBlockingQueue<>(10);
        private final BlockingQueue<Integer> output = new ArrayBlockingQueue<>(10);

        private IntComp(final int[] instructions) {
            this.instructions = instructions;
        }

        public boolean hasMoreInstructions() {
            return pos < instructions.length && !halted;
        }

        public void step() throws Exception {
            final int instr = instructions[pos];
            final Op opCode = Op.of(instr % 100);
            final int[] parameters = Arrays.copyOfRange(instructions, pos + 1, pos + 1 + opCode.parameters);
            final int[] paramModes = parseParamModes(instr, opCode.parameters);

            doOp(opCode, parameters, paramModes);
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
                final Integer input = this.input.take();
                write(params[0], modes[0], input);
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
                return;
            }

            pos += params.length + 1;
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

    private static String getTestInput() {
        return "3,12,6,12,15,1,13,14,13,4,13,99,-1,0,1,9";
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
