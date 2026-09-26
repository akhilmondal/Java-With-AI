import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/*
 * J07  Exceptions: checked vs unchecked, finally, try-with-resources, custom: runnable demo
 *
 * Read J07_Exceptions.md first. This file runs the same steps so you can see
 * them happen. The step numbers match the .md file.
 *
 * Run it:  java 01-java-core/J07_Exceptions.java
 *          (or click "Run" above main() in VS Code)
 */
public class J07_Exceptions {

    static String gatewayName;                    // never set, so it stays null (used in Step 3)

    public static void main(String[] args) {
        step("Step 2: a checked exception (the compiler forces you to handle it)");
        try {
            String config = readGatewayConfig();  // declared "throws IOException", so we must catch or declare
            System.out.println(config);
        } catch (IOException e) {
            System.out.println("caught " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        step("Step 3: unchecked exceptions (bugs in our own code)");
        try {
            // Dividing by zero on purpose. javac even warns about it with -Xlint:
            // "[divzero] division by zero".
            int perHead = 1000 / 0;
            System.out.println(perHead);
        } catch (ArithmeticException e) {
            System.out.println("ArithmeticException: " + e.getMessage());
        }
        try {
            System.out.println(gatewayName.length());   // gatewayName is null
        } catch (NullPointerException e) {
            System.out.println("NullPointerException: " + e.getMessage());
        }
        try {
            System.out.println(Integer.parseInt("12a"));  // not a number
        } catch (NumberFormatException e) {
            System.out.println("NumberFormatException: " + e.getMessage());
        }

        step("Step 4: try, catch, finally");
        try {
            System.out.println("try     : debit Rs 1500 from a balance of Rs 1000");
            debit(1000, 1500);
            System.out.println("try     : this line never runs");
        } catch (InsufficientBalanceException e) {
            System.out.println("catch   : " + e.getMessage());
        } finally {
            System.out.println("finally : always runs (close the payment log here)");
        }
        System.out.println("return in try (1) and in finally (2) -> method returns " + returnTrap());

        step("Step 5: try-with-resources closes things for you");
        try (DbConnection db = new DbConnection();          // opened first
             AuditLog log = new AuditLog(false)) {           // opened second
            db.save("TXN1001");
            log.write("debit TXN1001");
        }                                                    // closed automatically: log first, then db
        System.out.println("-- now the body fails AND close() fails --");
        try (AuditLog log = new AuditLog(true)) {
            log.write("debit TXN1002");
            throw new IllegalStateException("debit failed");
        } catch (IllegalStateException e) {
            System.out.println("  caught    : " + e.getMessage());
            for (Throwable s : e.getSuppressed()) {          // the close() error is kept, not lost
                System.out.println("  suppressed: " + s.getMessage());
            }
        }

        step("Step 6: a custom exception");
        try {
            debit(1000, 1500);
        } catch (InsufficientBalanceException e) {
            System.out.println(e.getClass().getSimpleName() + ": " + e.getMessage());
            System.out.println("short by Rs " + (e.amount - e.balance));
        }

        step("Step 7: wrap it, but keep the cause");
        try {
            loadGatewayOrFail();
        } catch (PaymentFailedException e) {
            System.out.println("message : " + e.getMessage());
            System.out.println("cause   : " + e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Methods used by the steps
    // -------------------------------------------------------------------------

    /** Reads a file that doesn't exist. IOException is CHECKED, so it must be declared. */
    static String readGatewayConfig() throws IOException {
        // Without "throws IOException", javac refuses to compile:
        // "unreported exception java.io.IOException; must be caught or declared to be thrown"
        return Files.readString(Path.of("config/missing-gateway.properties"));
    }

    static void debit(long balance, long amount) {
        if (amount > balance) {
            throw new InsufficientBalanceException(balance, amount);   // throw early, with a clear message
        }
        System.out.println("debited Rs " + amount);
    }

    /**
     * The finally trap, shown on purpose. A return inside finally REPLACES the
     * return from try (and would even hide an exception). javac warns about it
     * if you compile with -Xlint: "[finally] finally clause cannot complete normally".
     */
    @SuppressWarnings("finally")
    static int returnTrap() {
        try {
            return 1;
        } finally {
            return 2;                              // never return from finally in real code
        }
    }

    static void loadGatewayOrFail() {
        try {
            readGatewayConfig();
        } catch (IOException e) {
            // Wrap the low-level error in our own exception, and pass e as the cause.
            throw new PaymentFailedException("Could not load PayU gateway config", e);
        }
    }

    // -------------------------------------------------------------------------
    // Resources and custom exceptions
    // -------------------------------------------------------------------------

    static class DbConnection implements AutoCloseable {
        DbConnection() {
            System.out.println("  open  DB connection");
        }

        void save(String txnId) {
            System.out.println("  save  " + txnId);
        }

        @Override
        public void close() {
            System.out.println("  close DB connection");
        }
    }

    static class AuditLog implements AutoCloseable {
        private final boolean failOnClose;

        AuditLog(boolean failOnClose) {
            this.failOnClose = failOnClose;
            System.out.println("  open  audit log");
        }

        void write(String line) {
            System.out.println("  write " + line);
        }

        @Override
        public void close() {
            System.out.println("  close audit log");
            if (failOnClose) {
                throw new IllegalStateException("audit log close failed");
            }
        }
    }

    /** Unchecked (extends RuntimeException): a business rule was broken. */
    static class InsufficientBalanceException extends RuntimeException {
        private static final long serialVersionUID = 1L;   // exceptions are Serializable, so javac asks for this
        final long balance;
        final long amount;

        InsufficientBalanceException(long balance, long amount) {
            super("Balance " + balance + " is less than " + amount);
            this.balance = balance;
            this.amount = amount;
        }
    }

    /** Our own wrapper for "the payment couldn't happen", carrying the real cause inside. */
    static class PaymentFailedException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        PaymentFailedException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    static void step(String text) {
        System.out.println();
        System.out.println("=== " + text + " ===");
    }
}
