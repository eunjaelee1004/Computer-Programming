package problem4;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class Test {
    private static int passed = 0, failed = 0;

    private static String capture(Runnable r) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream old = System.out;
        System.setOut(new PrintStream(baos));
        r.run();
        System.setOut(old);
        return baos.toString().replaceAll("\\r\\n", "\n").replaceAll("\\n$", "");
    }

    private static void check(String name, String expected, String actual) {
        if (expected.equals(actual)) {
            passed++;
            System.out.println("PASS [" + name + "]");
        } else {
            failed++;
            System.out.println("FAIL [" + name + "]");
            System.out.println("  Expected: \"" + expected.replace("\n", "\\n") + "\"");
            System.out.println("  Actual:   \"" + actual.replace("\n", "\\n") + "\"");
        }
    }

    public static void main(String[] args) {
        check("abb",
            "a: 0 0\nb: 1 2",
            capture(() -> CharacterFinder.findCharacter("abb")));

        check("bbbaAabaccHadAHbcbHHAdcbbeHaH",
            "A: 4 20\nH: 10 28\na: 3 27\nb: 0 24\nc: 8 22\nd: 12 21\ne: 25 25",
            capture(() -> CharacterFinder.findCharacter("bbbaAabaccHadAHbcbHHAdcbbeHaH")));

        System.out.println("\n=== Problem 4: " + passed + " passed, " + failed + " failed ===");
    }
}
