package problem3;

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
        check("n=1", "*",
            capture(() -> DrawingFigure.drawFigure(1)));

        check("n=2", "***\n *",
            capture(() -> DrawingFigure.drawFigure(2)));

        check("n=3", "*****\n * *\n  *",
            capture(() -> DrawingFigure.drawFigure(3)));

        check("n=4", "*******\n *   *\n  * *\n   *",
            capture(() -> DrawingFigure.drawFigure(4)));

        System.out.println("\n=== Problem 3: " + passed + " passed, " + failed + " failed ===");
    }
}
