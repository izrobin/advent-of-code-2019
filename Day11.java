import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.stream.Stream;

class Day11 {

    private static final Map<Point, Integer> HULL = new HashMap<>();

    public static void main(final String[] args) throws Exception {
        final IntComp comp = new IntComp(getInstructions(getInput()));
        //Provide it 0 for Part1 of the puzzle, and 1 for Part2
        final Robot robot = new Robot(1);

        while (!comp.terminated) {
            final int currentColor = robot.inspectHullPanel();
            comp.addInput(currentColor);

            while (comp.hasMoreInstructions()) {
                comp.step();
            }
            final long newColor = comp.output.poll();
            final long turnDir = comp.output.poll();

            robot.paint((int) newColor);
            robot.turn((int) turnDir);
        }

        System.out.println("Unique hull pieces visited: " + HULL.keySet().size());

        final int minX = HULL.keySet().stream().mapToInt(p -> p.x).min().orElse(0);
        final int maxX = HULL.keySet().stream().mapToInt(p -> p.x).max().orElse(0);

        final int minY = HULL.keySet().stream().mapToInt(p -> p.y).min().orElse(0);
        final int maxY = HULL.keySet().stream().mapToInt(p -> p.y).max().orElse(0);

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                final int color = HULL.getOrDefault(new Point(x, y), 0);
                System.out.print(color == 1 ? '█' : '.');
            }
            System.out.println();
        }

    }

    private static class Robot {
        private int xPos = 0;
        private int yPos = 0;
        private Direction direction = Direction.U;
        private final int startingColor;

        private Robot(final int startingColor) {
            this.startingColor = startingColor;
        }

        int inspectHullPanel() {
            if (HULL.isEmpty()) {
                return startingColor;
            }
            return HULL.getOrDefault(new Point(xPos, yPos), 0);
        }

        void paint(final int color) {
            HULL.put(new Point(xPos, yPos), color);
        }

        void turn(final int dir) {
            int newOrdinal = dir == 0 ? direction.ordinal() - 1 : direction.ordinal() + 1;
            if (newOrdinal > Direction.values().length - 1) {
                newOrdinal = 0;
            } else if (newOrdinal < 0) {
                newOrdinal = Direction.values().length - 1;
            }
            direction = Direction.values()[newOrdinal];

            switch (direction) {
                case U:
                    yPos -= 1;
                    break;
                case R:
                    xPos += 1;
                    break;
                case D:
                    yPos += 1;
                    break;
                case L:
                    xPos -= 1;
            }
        }

        private enum Direction {
            U, R, D, L
        }
    }

    private static class Point {
        private final int x;
        private final int y;

        private Point(final int x, final int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Point)) return false;
            Point point = (Point) o;
            return x == point.x &&
                y == point.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
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
        return "3,8,1005,8,336,1106,0,11,0,0,0,104,1,104,0,3,8,102,-1,8,10,1001,10,1,10,4,10,108,1,8,10,4,10,101,0,8,"
            + "28,1006,0,36,1,2,5,10,1006,0,57,1006,0,68,3,8,102,-1,8,10,1001,10,1,10,4,10,108,0,8,10,4,10,1002,8,1,"
            + "63,2,6,20,10,1,106,7,10,2,9,0,10,3,8,102,-1,8,10,101,1,10,10,4,10,108,1,8,10,4,10,102,1,8,97,1006,0,"
            + "71,3,8,1002,8,-1,10,101,1,10,10,4,10,108,1,8,10,4,10,1002,8,1,122,2,105,20,10,3,8,1002,8,-1,10,1001,"
            + "10,1,10,4,10,108,0,8,10,4,10,101,0,8,148,2,1101,12,10,1006,0,65,2,1001,19,10,3,8,102,-1,8,10,1001,10,"
            + "1,10,4,10,108,0,8,10,4,10,101,0,8,181,3,8,1002,8,-1,10,1001,10,1,10,4,10,1008,8,0,10,4,10,1002,8,1,"
            + "204,2,7,14,10,2,1005,20,10,1006,0,19,3,8,102,-1,8,10,101,1,10,10,4,10,108,1,8,10,4,10,102,1,8,236,"
            + "1006,0,76,1006,0,28,1,1003,10,10,1006,0,72,3,8,1002,8,-1,10,101,1,10,10,4,10,108,0,8,10,4,10,102,1,8,"
            + "271,1006,0,70,2,107,20,10,1006,0,81,3,8,1002,8,-1,10,1001,10,1,10,4,10,108,1,8,10,4,10,1002,8,1,303,2,"
            + "3,11,10,2,9,1,10,2,1107,1,10,101,1,9,9,1007,9,913,10,1005,10,15,99,109,658,104,0,104,1,21101,0,"
            + "387508441896,1,21102,1,353,0,1106,0,457,21101,0,937151013780,1,21101,0,364,0,1105,1,457,3,10,104,0,"
            + "104,1,3,10,104,0,104,0,3,10,104,0,104,1,3,10,104,0,104,1,3,10,104,0,104,0,3,10,104,0,104,1,21102,"
            + "179490040923,1,1,21102,411,1,0,1105,1,457,21101,46211964123,0,1,21102,422,1,0,1106,0,457,3,10,104,0,"
            + "104,0,3,10,104,0,104,0,21101,838324716308,0,1,21101,0,445,0,1106,0,457,21102,1,868410610452,1,21102,1,"
            + "456,0,1106,0,457,99,109,2,22101,0,-1,1,21101,40,0,2,21101,0,488,3,21101,478,0,0,1106,0,521,109,-2,"
            + "2105,1,0,0,1,0,0,1,109,2,3,10,204,-1,1001,483,484,499,4,0,1001,483,1,483,108,4,483,10,1006,10,515,"
            + "1101,0,0,483,109,-2,2105,1,0,0,109,4,2101,0,-1,520,1207,-3,0,10,1006,10,538,21101,0,0,-3,22102,1,-3,1,"
            + "21202,-2,1,2,21101,0,1,3,21101,557,0,0,1105,1,562,109,-4,2105,1,0,109,5,1207,-3,1,10,1006,10,585,2207,"
            + "-4,-2,10,1006,10,585,22101,0,-4,-4,1106,0,653,21201,-4,0,1,21201,-3,-1,2,21202,-2,2,3,21102,604,1,0,"
            + "1106,0,562,21202,1,1,-4,21101,0,1,-1,2207,-4,-2,10,1006,10,623,21102,0,1,-1,22202,-2,-1,-2,2107,0,-3,"
            + "10,1006,10,645,21202,-1,1,1,21101,0,645,0,106,0,520,21202,-2,-1,-2,22201,-4,-2,-4,109,-5,2105,1,0";
    }
}
