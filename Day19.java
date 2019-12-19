import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.stream.Stream;

class Day19 {
    public static void main(final String[] args) throws Exception {
        // PART 1
        int count = 0;
        for (int y = 0; y < 100; y++) {
            for (int x = 0; x < 100; x++) {
                final int insideBeam = runProgram(x, y);
                System.out.print(insideBeam);
                count += insideBeam;
            }
            System.out.println();
        }

        System.out.println("PUZZLE PART 1: " + count);

        // PART 2

        final int boxSize = 100;
        final int boxSizeAdj = boxSize - 1;
        /*
            1   2
            +---+
            |   |
            +---+
            3   4
         */

        int x = 0;
        int y = boxSize;

        int finalX = 0;
        int finalY = 0;
        while (true) {
            final boolean corner3Fits = runProgram(x, y) == 1;
            final boolean corner4Fits = runProgram(x + boxSizeAdj, y) == 1;

            if (corner3Fits) {
                System.out.printf("CORNER 3 FITS AT (%d,%d)%n", x, y);
                if (corner4Fits) {
                    System.out.printf("BOTTOM LINE FITS: (%d,%d)%n", x, y);
                    // Bottom part of box fits
                    final boolean corner1Fits = runProgram(x, y - boxSizeAdj) == 1;
                    final boolean corner2Fits = runProgram(x + boxSizeAdj, y - boxSizeAdj) == 1;

                    System.out.printf(
                        "TRYING CORNER 1 AND 2 AT (%d,%d) (%d,%d)%n",
                        x,
                        y - boxSizeAdj,
                        x + boxSizeAdj,
                        y - boxSizeAdj
                    );

                    if (corner1Fits && corner2Fits) {
                        finalX = x;
                        finalY = y - boxSizeAdj;
                        break;
                    } else {
                        // No space for the "top line" (corner 1 and 2), move down one
                        y++;
                    }
                } else {
                    // No place to draw the "bottom line" (corner 3 and 4) on this row, move down one
                    y++;
                }
            } else {
                // We haven't found our corner 3 yet, move to the right
                x++;
            }
        }

        System.out.printf(
            "PUZZLE PART 2: TOP CORNER AT (%d,%d)%n",
            finalX,
            finalY
        );

        // Uncomment to draw beam with box:

        /*
        for (int yd = 0; yd < finalY + boxSizeAdj; yd++) {
            for (int xd = 0; xd < finalX + boxSizeAdj; xd++) {
                final int tractorBeam = runProgram(xd, yd);

                final boolean isBox = (yd >= finalY && yd < finalY + boxSizeAdj) && (xd >= finalX && xd < finalX + boxSizeAdj);
                System.out.print(isBox && tractorBeam == 1 ? "0" : tractorBeam == 1 ? "#" : ".");
            }
            System.out.println();
        }
        */
    }

    private static int runProgram(final int x, final int y) throws Exception {
        final IntComp comp = new IntComp(getInstructions(getInput()));

        comp.addInput(x);
        comp.addInput(y);
        while (comp.hasMoreInstructions()) {
            comp.step();
        }

        final Long output = comp.output.poll();
        return Math.toIntExact(output);
    }

    private static class IntComp {
        private int pos = 0;
        private int rbase = 0;
        private boolean halted = false;
        private boolean terminated = false;
        private long[] instructions;
        private final Queue<Long> input = new ArrayBlockingQueue<>(100);
        private final Queue<Long> output = new ArrayBlockingQueue<>(100);

        private IntComp(final long[] instructions) {
            this.instructions = instructions;
        }

        public boolean hasMoreInstructions() {
            return pos < instructions.length && !halted && !terminated;
        }

        public void step() throws Exception {
            final int instr = (int) instructions[pos];
            final Op opCode = Op.of(instr % 100);
            final long[] parameters = Arrays.copyOfRange(instructions, pos + 1, pos + 1 + opCode.parameters);
            final int[] paramModes = parseParamModes(instr, opCode.parameters);

            doOp(opCode, parameters, paramModes);
        }

        void addInput(final long inp) {
            this.input.add(inp);
            this.halted = false;
        }

        private void doOp(
            final Op opCode,
            final long[] params,
            final int[] modes
        ) throws Exception {
            if (Op.ADD == opCode) {
                final long result = read(params[0], modes[0]) + read(params[1], modes[1]);
                write(params[2], modes[2], result);
            } else if (Op.MUL == opCode) {
                final long result = read(params[0], modes[0]) * read(params[1], modes[1]);
                write(params[2], modes[2], result);
            } else if (Op.INPUT == opCode) {
                final Long input = this.input.poll();
                if (input == null) {
                    halted = true;
                } else {
                    write(params[0], modes[0], input);
                }
            } else if (Op.OUTPUT == opCode) {
                final long output = read(params[0], modes[0]);
                this.output.add(output);
            } else if (Op.JIT == opCode) {
                final boolean jump = read(params[0], modes[0]) > 0;
                if (jump) {
                    pos = (int) (read(params[1], modes[1]) - params.length - 1);
                }
            } else if (Op.JIF == opCode) {
                final boolean jump = read(params[0], modes[0]) == 0;
                if (jump) {
                    pos = (int) (read(params[1], modes[1]) - params.length - 1);
                }
            } else if (Op.LT == opCode) {
                final int bit = read(params[0], modes[0]) < read(params[1], modes[1]) ? 1 : 0;
                write(params[2], modes[2], bit);
            } else if (Op.EQ == opCode) {
                final int bit = read(params[0], modes[0]) == read(params[1], modes[1]) ? 1 : 0;
                write(params[2], modes[2], bit);
            } else if (Op.RBASE == opCode) {
                rbase += (int) read(params[0], modes[0]);
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

        long read(final long pos, final int mode) {
            switch (mode) {
                case 0:
                    if (pos >= instructions.length) {
                        instructions = Arrays.copyOf(instructions, (int) (pos + 1));
                    }
                    return instructions[(int) pos];
                case 1:
                    return pos;
                case 2:
                    return instructions[(int) (pos + rbase)];
            }
            throw new IllegalArgumentException("Unknown mode: " + mode);
        }

        void write(final long pos, final int mode, final long inp) {
            switch (mode) {
                case 0:
                case 1:
                    expandIfNeeded(pos);
                    instructions[(int) pos] = inp;
                    break;
                case 2:
                    expandIfNeeded(pos + rbase);
                    instructions[(int) (pos + rbase)] = inp;
                    break;
            }
        }

        private void expandIfNeeded(final long desiredPos) {
            if (instructions.length - 1 < desiredPos) {
                instructions = Arrays.copyOf(instructions, (int) (desiredPos + 1));
            }
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

        static Op of(final long opCode) {
            return Stream.of(values()).filter(o -> o.opCode == opCode)
                .findFirst()
                .orElse(HALT);
        }
    }

    private static long[] getInstructions(final String input) {
        return Arrays.stream(input.split(","))
            .mapToLong(Long::parseLong)
            .toArray();
    }

    private static String getInput() {
        return "109,424,203,1,21102,1,11,0,1106,0,282,21101,0,18,0,1105,1,259,1201,1,0,221,203,1,21101,31,0,0,1105,1,"
            + "282,21102,38,1,0,1105,1,259,21001,23,0,2,21201,1,0,3,21101,1,0,1,21102,57,1,0,1106,0,303,2102,1,1,222,"
            + "21001,221,0,3,20102,1,221,2,21101,259,0,1,21102,80,1,0,1106,0,225,21101,0,167,2,21101,0,91,0,1105,1,"
            + "303,2102,1,1,223,20102,1,222,4,21102,1,259,3,21102,1,225,2,21102,225,1,1,21102,1,118,0,1106,0,225,"
            + "21001,222,0,3,21102,1,93,2,21101,0,133,0,1105,1,303,21202,1,-1,1,22001,223,1,1,21101,148,0,0,1105,1,"
            + "259,2101,0,1,223,21001,221,0,4,20102,1,222,3,21102,21,1,2,1001,132,-2,224,1002,224,2,224,1001,224,3,"
            + "224,1002,132,-1,132,1,224,132,224,21001,224,1,1,21102,1,195,0,106,0,108,20207,1,223,2,21001,23,0,1,"
            + "21101,-1,0,3,21102,214,1,0,1106,0,303,22101,1,1,1,204,1,99,0,0,0,0,109,5,1202,-4,1,249,21202,-3,1,1,"
            + "21202,-2,1,2,21201,-1,0,3,21101,0,250,0,1105,1,225,22101,0,1,-4,109,-5,2106,0,0,109,3,22107,0,-2,-1,"
            + "21202,-1,2,-1,21201,-1,-1,-1,22202,-1,-2,-2,109,-3,2106,0,0,109,3,21207,-2,0,-1,1206,-1,294,104,0,99,"
            + "22101,0,-2,-2,109,-3,2106,0,0,109,5,22207,-3,-4,-1,1206,-1,346,22201,-4,-3,-4,21202,-3,-1,-1,22201,-4,"
            + "-1,2,21202,2,-1,-1,22201,-4,-1,1,22102,1,-2,3,21102,343,1,0,1105,1,303,1106,0,415,22207,-2,-3,-1,1206,"
            + "-1,387,22201,-3,-2,-3,21202,-2,-1,-1,22201,-3,-1,3,21202,3,-1,-1,22201,-3,-1,2,21201,-4,0,1,21102,384,"
            + "1,0,1106,0,303,1106,0,415,21202,-4,-1,-4,22201,-4,-3,-4,22202,-3,-2,-2,22202,-2,-4,-4,22202,-3,-2,-3,"
            + "21202,-4,-1,-2,22201,-3,-2,1,22102,1,1,-4,109,-5,2105,1,0";
    }
}
