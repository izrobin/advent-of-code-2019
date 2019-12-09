import java.util.Arrays;
import java.util.stream.IntStream;

class Day4 {
    public static void main(final String[] args) {
        final String[] range = getInput().split("-");
        final long matchesPart1 = stream(range)
            .filter(Day4::passwordMatchesRulesPart1)
            .count();

        System.out.println("# of matches (Part 1): " + matchesPart1);

        final long matchesPart2 = Day4.stream(range)
            .filter(Day4::passwordMatchesRulesPart1)
            .filter(Day4::passwordMatchesRulesPart2)
            .count();

        System.out.println("# of matches (Part 2): " + matchesPart2);
        System.out.println(passwordMatchesRulesPart2(112223));
    }

    private static IntStream stream(String[] range) {
        return IntStream.rangeClosed(
            Integer.parseInt(range[0]),
            Integer.parseInt(range[1])
        );
    }

    private static boolean passwordMatchesRulesPart1(final int password) {
        final char[] iStr = String.valueOf(password).toCharArray();
        boolean adjacentDigits = false;

        for (int j = 1; j < iStr.length; j++) {
            final int previousInt = Character.getNumericValue(iStr[j - 1]);
            final int currentInt = Character.getNumericValue(iStr[j]);

            if (!adjacentDigits) {
                adjacentDigits = currentInt == previousInt;
            }

            if (currentInt < previousInt) {
                return false;
            }
        }

        return adjacentDigits;
    }

    private static boolean passwordMatchesRulesPart2(final int password) {
        final char[] chars = String.valueOf(password).toCharArray();

        final int[] count = new int[10];
        for (char c : chars) {
            int aChar = Character.getNumericValue(c);
            ++count[aChar];
        }

        for (int i = 0; i < 10; i++) {
            if (count[i] == 2) {
                return true;
            }
        }
        return false;
    }

    private static String getInput() {
        return "156218-652527";
    }
}
