import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.stream.Stream;

class Day13 {

    public static void main(final String[] args) throws Exception {
        final long[] instructions = getInstructions(getInput());
        instructions[0] = 2; // Part 2
        final IntComp comp = new IntComp(instructions);

        while (!comp.terminated) {
            while (comp.hasMoreInstructions()) {
                comp.step();
            }
            final GameState gameState = drawScreen(comp.output);

            if (gameState.ballX > gameState.padX) {
                // Move right
                comp.addInput(1);
            } else if (gameState.ballX < gameState.padX) {
                comp.addInput(-1);
            } else {
                comp.addInput(0);
            }

            Thread.sleep(100);
        }
    }

    private static class GameState {
        private final int ballX;
        private final int padX;

        private GameState(final int ballX, final int padX) {
            this.ballX = ballX;
            this.padX = padX;
        }
    }

    private static GameState drawScreen(final List<Long> outputs) {
        final List<Pixel> pixels = new ArrayList<>();
        String segmentDisplay = "";

        int ballX = 0;
        int padX = 0;
        for (int i = 0; i < outputs.size(); i++) {
            final long x = outputs.get(i++);
            final long y = outputs.get(i++);
            final long tile = outputs.get(i);
            if (x == -1 && y == 0) {
                segmentDisplay = String.valueOf(tile);
                continue;
            } else if (tile == 3) {
                padX = (int) x;
            } else if (tile == 4) {
                ballX = (int) x;
            }

            pixels.add(new Pixel(x, y, tile));
        }

        final int maxX = pixels.stream().mapToInt(p -> p.x).max().orElse(0);
        final int maxY = pixels.stream().mapToInt(p -> p.y).max().orElse(0);
        final int[][] screen = new int[maxY + 1][maxX + 1];

        System.out.print("\33[" + maxX + 1 + "A");

        for (final Pixel pixel : pixels) {
            screen[pixel.y][pixel.x] = pixel.tile;
        }

        int blocks = 0;
        for (int[] linePxs : screen) {
            for (int block : linePxs) {
                blocks += block == 2 ? 1 : 0;
                System.out.print(getProp(block));
            }
            System.out.println();
        }

        System.out.println("Total blocks: " + blocks);
        System.out.println("Segment: " + segmentDisplay);
        return new GameState(ballX, padX);
    }

    private static String getProp(final int propNbr) {
        switch (propNbr) {
            case 0:
            default:
                return "   ";
            case 1:
                return "███";
            case 2:
                return "░░░";
            case 3:
                return "▄▄▄";
            case 4:
                return " ■ ";
        }
    }

    private static class Pixel {
        private final int x;
        private final int y;
        private final int tile;

        private Pixel(final long x, final long y, final long tile) {
            this.x = (int) x;
            this.y = (int) y;
            this.tile = (int) tile;
        }
    }

    private static class IntComp {
        private int pos = 0;
        private int rbase = 0;
        private boolean halted = false;
        private boolean terminated = false;
        private long[] instructions;
        private final Queue<Long> input = new ArrayBlockingQueue<>(100);
        private final List<Long> output = new ArrayList<>();

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
        return "1,380,379,385,1008,2249,380030,381,1005,381,12,99,109,2250,1102,1,0,383,1101,0,0,382,20101,0,382,1,"
            + "20101,0,383,2,21102,1,37,0,1106,0,578,4,382,4,383,204,1,1001,382,1,382,1007,382,35,381,1005,381,22,"
            + "1001,383,1,383,1007,383,23,381,1005,381,18,1006,385,69,99,104,-1,104,0,4,386,3,384,1007,384,0,381,"
            + "1005,381,94,107,0,384,381,1005,381,108,1105,1,161,107,1,392,381,1006,381,161,1102,1,-1,384,1105,1,119,"
            + "1007,392,33,381,1006,381,161,1102,1,1,384,20101,0,392,1,21102,21,1,2,21102,0,1,3,21101,0,138,0,1105,1,"
            + "549,1,392,384,392,21002,392,1,1,21102,21,1,2,21101,3,0,3,21102,1,161,0,1105,1,549,1101,0,0,384,20001,"
            + "388,390,1,20101,0,389,2,21102,180,1,0,1105,1,578,1206,1,213,1208,1,2,381,1006,381,205,20001,388,390,1,"
            + "21002,389,1,2,21102,1,205,0,1105,1,393,1002,390,-1,390,1102,1,1,384,21001,388,0,1,20001,389,391,2,"
            + "21101,0,228,0,1105,1,578,1206,1,261,1208,1,2,381,1006,381,253,21001,388,0,1,20001,389,391,2,21101,0,"
            + "253,0,1106,0,393,1002,391,-1,391,1101,0,1,384,1005,384,161,20001,388,390,1,20001,389,391,2,21101,0,"
            + "279,0,1106,0,578,1206,1,316,1208,1,2,381,1006,381,304,20001,388,390,1,20001,389,391,2,21102,304,1,0,"
            + "1105,1,393,1002,390,-1,390,1002,391,-1,391,1101,0,1,384,1005,384,161,20101,0,388,1,21001,389,0,2,"
            + "21102,1,0,3,21102,1,338,0,1106,0,549,1,388,390,388,1,389,391,389,20101,0,388,1,21001,389,0,2,21102,1,"
            + "4,3,21101,365,0,0,1106,0,549,1007,389,22,381,1005,381,75,104,-1,104,0,104,0,99,0,1,0,0,0,0,0,0,260,15,"
            + "18,1,1,17,109,3,21202,-2,1,1,21202,-1,1,2,21101,0,0,3,21102,414,1,0,1106,0,549,21202,-2,1,1,22101,0,"
            + "-1,2,21102,1,429,0,1106,0,601,1201,1,0,435,1,386,0,386,104,-1,104,0,4,386,1001,387,-1,387,1005,387,"
            + "451,99,109,-3,2105,1,0,109,8,22202,-7,-6,-3,22201,-3,-5,-3,21202,-4,64,-2,2207,-3,-2,381,1005,381,492,"
            + "21202,-2,-1,-1,22201,-3,-1,-3,2207,-3,-2,381,1006,381,481,21202,-4,8,-2,2207,-3,-2,381,1005,381,518,"
            + "21202,-2,-1,-1,22201,-3,-1,-3,2207,-3,-2,381,1006,381,507,2207,-3,-4,381,1005,381,540,21202,-4,-1,-1,"
            + "22201,-3,-1,-3,2207,-3,-4,381,1006,381,529,21202,-3,1,-7,109,-8,2105,1,0,109,4,1202,-2,35,566,201,-3,"
            + "566,566,101,639,566,566,2101,0,-1,0,204,-3,204,-2,204,-1,109,-4,2105,1,0,109,3,1202,-1,35,593,201,-2,"
            + "593,593,101,639,593,593,21001,0,0,-2,109,-3,2105,1,0,109,3,22102,23,-2,1,22201,1,-1,1,21101,0,409,2,"
            + "21102,1,437,3,21102,1,805,4,21102,1,630,0,1106,0,456,21201,1,1444,-2,109,-3,2106,0,0,1,1,1,1,1,1,1,1,"
            + "1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,"
            + "0,0,0,0,0,0,0,0,0,0,1,1,0,2,0,2,0,0,0,2,2,0,0,2,0,2,2,2,0,2,2,0,0,2,2,0,0,2,2,2,2,2,2,2,0,1,1,0,0,2,2,"
            + "0,0,0,2,2,2,0,0,0,2,2,2,0,2,0,2,0,2,0,2,0,2,0,2,2,2,2,2,0,1,1,0,0,2,2,2,2,0,2,0,2,2,2,2,2,0,0,0,2,0,2,"
            + "2,0,2,0,0,2,2,0,2,2,0,0,0,1,1,0,2,0,2,2,2,2,0,0,0,2,2,0,0,2,2,2,0,2,2,2,2,0,2,2,0,0,2,0,2,0,0,0,1,1,0,"
            + "2,0,2,2,0,2,2,0,0,0,0,2,0,2,2,2,0,2,0,0,2,2,2,0,0,0,0,2,2,2,0,0,1,1,0,2,0,0,0,0,0,2,0,2,0,2,0,2,0,0,2,"
            + "2,2,0,0,0,2,0,0,0,0,0,2,2,2,2,0,1,1,0,2,2,2,2,0,0,0,0,2,2,2,0,2,2,0,0,2,2,0,2,2,0,2,0,2,2,0,2,2,0,2,0,"
            + "1,1,0,2,0,0,2,2,0,2,2,2,0,2,2,0,0,2,0,0,0,0,2,2,0,2,2,2,2,2,0,2,2,0,0,1,1,0,0,0,0,2,2,0,0,2,2,0,0,0,2,"
            + "2,2,2,0,2,0,2,2,2,0,2,0,0,0,0,0,0,2,0,1,1,0,0,2,2,0,0,0,2,0,2,2,2,0,0,2,2,2,2,2,0,0,2,2,0,2,2,0,0,0,2,"
            + "2,0,0,1,1,0,2,0,0,0,0,0,2,2,2,0,0,2,0,2,2,0,2,2,0,2,0,2,2,2,2,2,2,2,0,2,0,0,1,1,0,0,2,2,0,0,2,2,2,0,2,"
            + "2,0,2,2,2,2,2,0,0,0,0,2,0,2,0,2,0,2,2,2,0,0,1,1,0,2,2,2,0,2,2,2,2,2,0,0,2,0,0,0,2,0,2,0,0,2,2,0,2,2,2,"
            + "2,2,0,0,2,0,1,1,0,2,0,0,2,2,0,0,0,0,0,2,0,0,2,2,2,2,2,0,2,2,0,2,0,0,2,0,2,0,2,2,0,1,1,0,2,2,0,2,2,2,0,"
            + "2,0,2,0,0,0,2,2,2,2,0,2,2,0,2,0,0,2,0,2,0,2,2,2,0,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,"
            + "0,0,0,0,0,0,0,0,0,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,1,0,0,0,0,0,"
            + "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,"
            + "0,0,0,0,0,0,0,0,0,0,0,0,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,3,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,1,0,0,"
            + "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,50,83,17,51,29,67,27,74,3,96,21,38,14,"
            + "55,71,75,15,64,69,72,2,30,70,52,29,61,96,52,48,79,27,36,90,39,21,41,55,56,8,7,13,5,39,49,22,52,66,77,"
            + "95,3,46,35,75,31,31,96,86,23,72,71,27,20,6,58,70,37,48,67,24,58,27,92,29,82,30,53,76,42,54,65,62,4,57,"
            + "20,42,57,25,16,76,48,77,36,61,22,31,65,88,7,50,34,54,7,1,38,62,62,83,33,70,73,46,14,89,23,98,14,28,75,"
            + "79,15,3,98,79,3,4,28,22,5,73,63,60,66,45,36,96,80,48,23,97,98,79,79,82,45,72,6,68,17,51,13,34,27,95,"
            + "84,59,41,40,40,13,86,21,92,37,30,54,59,48,94,63,37,53,2,62,87,47,42,28,60,48,97,61,68,59,39,31,22,88,"
            + "34,72,54,11,89,34,68,35,71,5,68,97,37,43,41,80,42,39,91,94,41,56,18,10,76,69,39,4,4,11,14,32,45,85,65,"
            + "57,51,72,70,53,71,29,9,78,24,31,16,9,63,40,10,26,73,69,45,72,15,98,83,59,3,21,72,97,19,74,69,61,61,62,"
            + "55,91,28,4,25,27,61,89,91,16,56,11,63,93,25,88,17,12,44,69,92,90,41,6,30,3,89,1,17,21,56,93,2,47,14,"
            + "14,92,21,52,83,36,36,11,97,6,57,53,97,88,48,70,53,94,84,79,56,2,35,36,68,18,97,60,75,85,30,4,89,14,45,"
            + "13,88,41,16,59,52,8,47,50,76,93,36,87,22,65,36,32,56,63,31,97,51,70,4,49,37,5,27,48,16,48,79,92,55,3,"
            + "94,35,46,79,4,92,46,22,87,21,88,50,36,82,67,40,63,97,69,91,63,98,68,2,17,3,87,59,71,87,18,30,13,86,87,"
            + "84,39,14,63,49,83,57,5,66,11,61,81,9,81,52,62,47,32,86,28,96,4,57,4,57,95,91,71,91,57,1,16,46,40,38,"
            + "62,7,85,87,76,22,43,23,77,85,73,37,37,90,53,7,25,30,57,98,73,66,56,48,19,74,53,4,65,38,94,9,22,55,67,"
            + "89,81,96,36,42,3,17,73,28,56,40,42,72,28,20,4,49,2,14,18,10,34,78,13,13,65,6,55,47,97,37,24,51,88,42,"
            + "22,60,35,2,10,27,37,13,51,53,24,26,81,62,68,30,25,34,9,29,51,6,22,76,21,40,38,97,7,64,31,80,64,10,89,"
            + "69,50,64,74,94,22,75,30,41,48,58,77,70,48,22,86,10,35,82,84,8,23,28,21,79,98,43,34,19,71,39,80,35,37,"
            + "81,33,8,35,56,68,23,2,38,32,32,86,60,37,42,53,10,16,5,45,92,20,78,90,25,19,94,44,7,81,22,3,4,37,14,26,"
            + "3,42,92,22,44,58,28,63,41,81,94,85,2,96,63,67,87,42,55,27,22,94,14,86,19,88,65,93,91,11,47,67,98,28,6,"
            + "43,46,41,33,27,84,96,39,40,54,81,39,68,85,79,48,59,27,68,34,51,36,64,8,54,44,17,58,54,83,17,56,79,57,"
            + "5,52,25,8,73,23,63,89,91,72,74,4,12,97,67,6,67,88,52,92,97,28,75,85,64,29,20,5,35,7,54,38,14,93,62,59,"
            + "74,93,86,91,82,23,83,1,35,5,21,18,71,7,39,8,32,68,57,95,67,39,19,98,89,17,87,37,78,54,36,22,30,35,68,"
            + "95,61,31,72,86,85,33,12,81,91,1,23,63,91,34,5,86,70,65,69,72,20,84,38,13,94,47,22,40,85,15,18,95,26,"
            + "68,63,59,38,73,24,69,31,21,87,90,66,87,84,30,79,76,55,33,55,33,94,7,55,380030";
    }
}
