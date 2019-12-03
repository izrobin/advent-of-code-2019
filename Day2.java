import java.util.Arrays;

class Day2 {

    private static final boolean PUZZLE_PART_2 = true;
    private static final int EXPECTED_RESULT = 19690720;

    public static void main(final String[] args) {
        if (!PUZZLE_PART_2) {
            final int result = runProgram(12, 2, getInstructions());
            System.out.println(result);
            System.exit(0);
        }

        for (int noun = 0; noun < 100; noun++) {
            for (int verb = 0; verb < 100; verb++) {
                final int result = runProgram(noun, verb, getInstructions());

                if (result == EXPECTED_RESULT) {
                    System.out.println("WINNING: " + result);
                    System.out.println("noun: " + noun);
                    System.out.println("verb: " + verb);
                    System.exit(0);
                }
            }
        }
    }

    private static int runProgram(
        final int noun,
        final int verb,
        int[] instructions
    ) {
        //Set init values
        instructions[1] = noun;
        instructions[2] = verb;

        int pos = 0;
        while (pos < instructions.length) {
            final int currentInstruction = instructions[pos];
            if (currentInstruction == 99) {
                break;
            }

            final int pos1 = instructions[pos + 1];
            final int pos2 = instructions[pos + 2];
            final int posResult = instructions[pos + 3];

            final int val1 = instructions[pos1];
            final int val2 = instructions[pos2];

            final int result =
                currentInstruction == 1 ? val1 + val2 :
                    currentInstruction == 2 ? val1 * val2 : 0;

            instructions[posResult] = result;
            pos += 4;
        }

        return instructions[0];
    }

    private static int[] getInstructions() {
        return Arrays.stream(getInput().split(","))
            .mapToInt(Integer::parseInt)
            .toArray();
    }

    private static String getInput() {
        return "1,0,0,3,1,1,2,3,1,3,4,3,1,5,0,3,2,13,1,19,1,19,10,23,2,10,23,27,1,27,6,31,1,13,31,35,1,13,35,39,1,39,"
            + "10,43,2,43,13,47,1,47,9,51,2,51,13,55,1,5,55,59,2,59,9,63,1,13,63,67,2,13,67,71,1,71,5,75,2,75,13,79,"
            + "1,79,6,83,1,83,5,87,2,87,6,91,1,5,91,95,1,95,13,99,2,99,6,103,1,5,103,107,1,107,9,111,2,6,111,115,1,5,"
            + "115,119,1,119,2,123,1,6,123,0,99,2,14,0,0";
    }
}
