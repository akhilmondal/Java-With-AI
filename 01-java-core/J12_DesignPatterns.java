import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * J12  Design patterns: Singleton, Builder, Factory, Strategy (+ Proxy): runnable demo
 *
 * Read J12_DesignPatterns.md first. This file runs the same steps so you can
 * see them happen. The step numbers match the .md file.
 *
 * Run it:  java 01-java-core/J12_DesignPatterns.java
 *          (or click "Run" above main() in VS Code)
 */
public class J12_DesignPatterns {

    public static void main(String[] args) throws InterruptedException {
        step("Step 1: Singleton, two threads call getInstance() at the same time");
        runTogether(LazyUnsafeConfig::getInstance, LazyUnsafeConfig::getInstance);
        System.out.println("lazy, no locking      : created " + LazyUnsafeConfig.created.get() + " instances  <- WRONG");
        runTogether(DoubleCheckedConfig::getInstance, DoubleCheckedConfig::getInstance);
        System.out.println("double-checked locking: created " + DoubleCheckedConfig.created.get() + " instance");
        runTogether(HolderConfig::getInstance, HolderConfig::getInstance);
        System.out.println("holder class          : created " + HolderConfig.created.get() + " instance");
        EnumConfig first = EnumConfig.INSTANCE;                  // fetched in two different places...
        EnumConfig second = EnumConfig.INSTANCE;
        System.out.println("enum                  : same object? " + (first == second)   // ...still one object
                + ", url = " + first.gatewayUrl());

        step("Step 2: Builder");
        PaymentRequest request = PaymentRequest.builder("TXN1001", 1500)   // required fields first
                .gateway("SETU")                                            // optional fields by name
                .remarks("electricity bill")
                .build();                                                   // currency not set: default INR
        System.out.println(request);
        try {
            PaymentRequest.builder("TXN1002", 0).build();                   // build() checks the rules
        } catch (IllegalArgumentException e) {
            System.out.println("build() refused: " + e.getMessage());
        }

        step("Step 3: Factory");
        PaymentGateway gateway = PaymentGatewayFactory.get("SETU");        // the caller never writes "new"
        System.out.println("factory gave " + gateway.getClass().getSimpleName() + " -> " + gateway.pay(1500));
        try {
            PaymentGatewayFactory.get("STRIPE");
        } catch (IllegalArgumentException e) {
            System.out.println("factory refused: " + e.getMessage());
        }

        step("Step 4: Strategy, same payment, fee rule chosen at runtime");
        for (String mode : List.of("UPI", "CARD", "NETBANKING")) {
            FeeStrategy strategy = feeStrategyFor(mode);
            int fee = strategy.fee(1000);
            System.out.println(mode + ": fee Rs " + fee + ", total Rs " + (1000 + fee));
        }

        step("Step 5: Proxy, how Spring's @Transactional wraps your method");
        PaymentGateway real = new PayUGateway();
        PaymentGateway proxy = (PaymentGateway) Proxy.newProxyInstance(
                PaymentGateway.class.getClassLoader(),
                new Class<?>[] {PaymentGateway.class},
                (proxyObject, method, methodArgs) -> {             // runs INSTEAD of every call on the proxy
                    System.out.println("  [proxy] begin transaction before " + method.getName() + "()");
                    Object result = method.invoke(real, methodArgs);   // then calls the real object
                    System.out.println("  [proxy] commit transaction after " + method.getName() + "()");
                    return result;
                });
        System.out.println("  result: " + proxy.pay(1500));
    }

    // =========================================================================
    // Step 1: Singleton, four ways
    // =========================================================================

    /** BROKEN on purpose: two threads can both see null and both create one. */
    static class LazyUnsafeConfig {
        static final AtomicInteger created = new AtomicInteger();
        private static LazyUnsafeConfig instance;

        private LazyUnsafeConfig() {                  // private: nobody else can call "new"
            created.incrementAndGet();
        }

        static LazyUnsafeConfig getInstance() {
            if (instance == null) {                   // both threads can pass this check...
                pause(50);                            // (a short delay makes the race easy to see)
                instance = new LazyUnsafeConfig();    // ...and both create an instance
            }
            return instance;
        }
    }

    /** Double-checked locking: lock only the first time, and check again inside the lock. */
    static class DoubleCheckedConfig {
        static final AtomicInteger created = new AtomicInteger();
        private static volatile DoubleCheckedConfig instance;   // volatile: nobody sees a half-built object (J05)

        private DoubleCheckedConfig() {
            created.incrementAndGet();
        }

        static DoubleCheckedConfig getInstance() {
            if (instance == null) {                          // 1st check: no locking once it exists
                synchronized (DoubleCheckedConfig.class) {
                    if (instance == null) {                  // 2nd check: the other thread may have made it
                        pause(50);
                        instance = new DoubleCheckedConfig();
                    }
                }
            }
            return instance;
        }
    }

    /** Holder class: the JVM creates INSTANCE the first time Holder is used, safely, once. */
    static class HolderConfig {
        static final AtomicInteger created = new AtomicInteger();

        private HolderConfig() {
            created.incrementAndGet();
        }

        private static class Holder {
            static final HolderConfig INSTANCE = new HolderConfig();
        }

        static HolderConfig getInstance() {
            return Holder.INSTANCE;
        }
    }

    /** Enum: the simplest thread-safe singleton; reflection and serialization can't make a second one. */
    enum EnumConfig {
        INSTANCE;

        String gatewayUrl() {
            return "https://api.payu.example";
        }
    }

    // =========================================================================
    // Step 2: Builder
    // =========================================================================
    static final class PaymentRequest {
        private final String txnId;
        private final int amount;
        private final String currency;
        private final String gateway;
        private final String remarks;

        private PaymentRequest(Builder b) {           // only the Builder can create it
            this.txnId = b.txnId;
            this.amount = b.amount;
            this.currency = b.currency;
            this.gateway = b.gateway;
            this.remarks = b.remarks;
        }

        static Builder builder(String txnId, int amount) {   // required fields go here
            return new Builder(txnId, amount);
        }

        @Override
        public String toString() {
            return "PaymentRequest[txnId=" + txnId + ", amount=" + amount + ", currency=" + currency
                    + ", gateway=" + gateway + ", remarks=" + remarks + "]";
        }

        static final class Builder {
            private final String txnId;
            private final int amount;
            private String currency = "INR";          // defaults for the optional fields
            private String gateway = "PAYU";
            private String remarks = "";

            private Builder(String txnId, int amount) {
                this.txnId = txnId;
                this.amount = amount;
            }

            Builder currency(String currency) {
                this.currency = currency;
                return this;                          // returning "this" is what lets you chain calls
            }

            Builder gateway(String gateway) {
                this.gateway = gateway;
                return this;
            }

            Builder remarks(String remarks) {
                this.remarks = remarks;
                return this;
            }

            PaymentRequest build() {
                if (amount <= 0) {
                    throw new IllegalArgumentException("amount must be positive, got " + amount);
                }
                return new PaymentRequest(this);
            }
        }
    }

    // =========================================================================
    // Steps 3 and 5: Factory, and the gateways it creates
    // =========================================================================
    interface PaymentGateway {
        String pay(int amount);
    }

    static class PayUGateway implements PaymentGateway {
        @Override
        public String pay(int amount) {
            return "PAYU-OK-" + amount;
        }
    }

    static class SetuGateway implements PaymentGateway {
        @Override
        public String pay(int amount) {
            return "SETU-OK-" + amount;
        }
    }

    /** The one place that knows which class to create for which name. */
    static class PaymentGatewayFactory {
        static PaymentGateway get(String name) {
            return switch (name) {
                case "PAYU" -> new PayUGateway();
                case "SETU" -> new SetuGateway();
                default -> throw new IllegalArgumentException("Unknown gateway: " + name);
            };
        }
    }

    // =========================================================================
    // Step 4: Strategy
    // =========================================================================
    @FunctionalInterface
    interface FeeStrategy {
        int fee(int amount);
    }

    /** Each payment mode has its own fee rule; the caller just asks for the right one. */
    static FeeStrategy feeStrategyFor(String mode) {
        return switch (mode) {
            case "UPI" -> amount -> 0;                        // free
            case "CARD" -> amount -> amount * 2 / 100;        // 2%
            case "NETBANKING" -> amount -> 10;                // flat Rs 10
            default -> throw new IllegalArgumentException("Unknown mode: " + mode);
        };
    }

    // =========================================================================
    // Helpers
    // =========================================================================
    static void runTogether(Runnable first, Runnable second) throws InterruptedException {
        Thread t1 = new Thread(first);
        Thread t2 = new Thread(second);
        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }

    static void pause(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static void step(String text) {
        System.out.println();
        System.out.println("=== " + text + " ===");
    }
}
