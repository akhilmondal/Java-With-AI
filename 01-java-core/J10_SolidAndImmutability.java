import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * J10  SOLID, interface vs abstract class, immutable class: a runnable demo
 *
 * THE STORY (why these rules exist)
 *   One giant class, every change is risky      -> SOLID, one pain per letter  (around 2000)
 *   A new interface method broke every class    -> default methods             (Java 8)
 *   Shared objects changed behind your back     -> immutable classes
 *   About 40 lines of boilerplate per class     -> records                     (Java 16)
 *
 * WHAT YOU WILL SEE (the numbers match J10_SolidAndImmutability.md)
 *   Step 1  S: validator, gateway, repository and notifier each do one job
 *   Step 2  O: PayU 1020, Setu 1005, then Razorpay 1015 added as a NEW class only
 *   Step 3  L: payBill() works for a savings account (3800) but breaks for a fixed deposit
 *   Step 4  I: only gateways that implement Refundable get asked to refund
 *   Step 5  D: the service accepts a fake gateway, because it depends on the interface
 *   Step 6  an abstract class keeps state (calls = 2); an interface has a default method
 *   Step 7  immutable: a defensive copy stays [UPI, BBPS]; the leaky one becomes [UPI, BBPS, REFUND]
 *
 * The gateway fees are made up for the example: PayU 2%, Setu a flat Rs 5,
 * Razorpay 1.5%.
 *
 * HOW TO RUN   java 01-java-core/J10_SolidAndImmutability.java   (or click "Run" above main)
 * READ FIRST   J10_SolidAndImmutability.md
 */
public class J10_SolidAndImmutability {

    public static void main(String[] args) {
        // Step 1 (S): the checkout only coordinates; each job lives in its own class.
        step("Step 1: S, single responsibility (one class, one job)");
        CheckoutService checkout = new CheckoutService(new PaymentValidator(), new PayUGateway(),
                new PaymentRepository(), new NotificationService());
        checkout.checkout("TXN1001", 1000);

        // Step 2 (O): a new gateway is a new class. PaymentService is never edited.
        step("Step 2: O, open for extension, closed for modification");
        PaymentService service = new PaymentService(List.of(new PayUGateway(), new SetuGateway()));
        System.out.println(service.pay("PAYU", 1000));   // 1000 + 2% = 1020
        System.out.println(service.pay("SETU", 1000));   // 1000 + 5 = 1005
        service = new PaymentService(List.of(new PayUGateway(), new SetuGateway(), new RazorpayGateway()));
        System.out.println(service.pay("RAZORPAY", 1000)); // 1000 + 1.5% = 1015
        System.out.println("Notice: Razorpay was added as a new class; no existing class changed.");

        // Step 3 (L): payBill() is written for ANY Account. A subclass that can't
        // withdraw breaks it.
        step("Step 3: L, a subclass must work wherever the parent works");
        Account savings = new Account(5000);
        payBill(savings, 1200);
        System.out.println("savings balance: " + savings.balance);          // 3800
        try {
            payBill(new FixedDepositAccount(5000), 1200);                    // written for ANY Account...
        } catch (UnsupportedOperationException e) {
            System.out.println("FixedDepositAccount broke payBill(): " + e.getMessage());
        }

        // Step 4 (I): refund is its own small interface, so Setu isn't forced to fake it.
        step("Step 4: I, small interfaces, so nobody implements what they can't do");
        for (PaymentGateway gateway : List.of(new PayUGateway(), new SetuGateway(), new RazorpayGateway())) {
            // "instanceof Refundable refundable" (Java 16+) checks the type and names it in one go
            if (gateway instanceof Refundable refundable) {
                System.out.println(gateway.name() + ": " + refundable.refund("P1"));
            } else {
                System.out.println(gateway.name() + ": no refunds (doesn't implement Refundable)");
            }
        }

        // Step 5 (D): the service depends on the PaymentGateway interface, so any
        // implementation plugs in, including a fake for tests.
        step("Step 5: D, depend on the interface, so you can plug in anything");
        PaymentService testService = new PaymentService(List.of(new FakeGateway()));
        System.out.println(testService.pay("FAKE", 1000) + "  (a unit test needs no real PayU call)");

        // Step 6: an abstract class can hold STATE and shared code; an interface can't hold state.
        step("Step 6: interface vs abstract class");
        PayUGateway payu = new PayUGateway();
        payu.pay(1000);
        payu.pay(500);
        System.out.println("BaseGateway (abstract class) keeps state: calls = " + payu.calls());   // 2
        System.out.println("PaymentGateway (interface) default method: " + payu.describe());

        // Step 7: an immutable class copies what it's given, so the caller can't change it later.
        step("Step 7: an immutable class");
        List<String> tags = new ArrayList<>(List.of("UPI", "BBPS"));
        LeakyPaymentRequest leaky = new LeakyPaymentRequest(tags);
        PaymentRequest safe = new PaymentRequest("TXN1001", 1000, tags);
        tags.add("REFUND");                            // the caller changes ITS list afterwards
        System.out.println("leaky tags: " + leaky.tags() + "  (changed from outside!)");
        System.out.println("safe tags : " + safe.tags() + "  (defensive copy, unchanged)");
        try {
            safe.tags().add("HACK");
        } catch (UnsupportedOperationException e) {
            System.out.println("safe.tags().add(...) -> UnsupportedOperationException");
        }
        PaymentRequest bigger = safe.withAmount(1500); // a "change" makes a NEW object
        System.out.println("safe.amount() = " + safe.amount() + ", bigger.amount() = " + bigger.amount());
        System.out.println("Notice: final alone isn't enough; copy mutable inputs (List.copyOf).");
    }

    // =========================================================================
    // Step 1: one job per class
    // =========================================================================
    static class PaymentValidator {
        void validate(int amount) {
            if (amount <= 0) {
                throw new IllegalArgumentException("amount must be positive");
            }
            System.out.println("  PaymentValidator   : amount " + amount + " is valid");
        }
    }

    static class PaymentRepository {
        void save(String txnId, String result) {
            System.out.println("  PaymentRepository  : saved " + txnId + " -> " + result);
        }
    }

    static class NotificationService {
        void sms(String txnId) {
            System.out.println("  NotificationService: SMS sent for " + txnId);
        }
    }

    /** Only coordinates. Each job lives in its own class, so each changes for one reason. */
    static class CheckoutService {
        private final PaymentValidator validator;
        private final PaymentGateway gateway;
        private final PaymentRepository repository;
        private final NotificationService notifier;

        CheckoutService(PaymentValidator validator, PaymentGateway gateway,
                        PaymentRepository repository, NotificationService notifier) {
            this.validator = validator;
            this.gateway = gateway;
            this.repository = repository;
            this.notifier = notifier;
        }

        void checkout(String txnId, int amount) {
            validator.validate(amount);
            String result = gateway.pay(amount);
            System.out.println("  PaymentGateway     : " + result);
            repository.save(txnId, result);
            notifier.sms(txnId);
        }
    }

    // =========================================================================
    // Steps 2, 4, 5, 6: gateways
    // =========================================================================

    /** The contract every gateway follows. An interface holds no state. */
    interface PaymentGateway {
        String name();

        int fee(int amount);

        String pay(int amount);

        /** A default method (Java 8+): an interface can share behaviour, but still no fields. */
        default String describe() {
            return name() + " charges Rs " + fee(1000) + " on Rs 1000";
        }
    }

    /** I: a separate, small interface, only for gateways that CAN refund. */
    interface Refundable {
        String refund(String paymentId);
    }

    /**
     * An abstract class: shared code AND state (the calls counter) for all real gateways.
     * pay() is the fixed recipe; each gateway fills in only callApi().
     */
    abstract static class BaseGateway implements PaymentGateway {
        private int calls;                            // state: an interface can't have this

        @Override
        public final String pay(int amount) {
            calls++;
            return callApi(amount + fee(amount));    // the step each gateway does differently
        }

        protected abstract String callApi(int total);

        int calls() {
            return calls;
        }
    }

    static class PayUGateway extends BaseGateway implements Refundable {
        @Override
        public String name() {
            return "PAYU";
        }

        @Override
        public int fee(int amount) {
            return amount * 2 / 100;                  // 2%
        }

        @Override
        protected String callApi(int total) {
            return "PAYU-OK-" + total;
        }

        @Override
        public String refund(String paymentId) {
            return "refunded " + paymentId;
        }
    }

    static class SetuGateway extends BaseGateway {    // no refunds, so it doesn't implement Refundable
        @Override
        public String name() {
            return "SETU";
        }

        @Override
        public int fee(int amount) {
            return 5;                                 // a flat Rs 5
        }

        @Override
        protected String callApi(int total) {
            return "SETU-OK-" + total;
        }
    }

    /** Added "later": a brand-new class, and no existing class had to change (O). */
    static class RazorpayGateway extends BaseGateway implements Refundable {
        @Override
        public String name() {
            return "RAZORPAY";
        }

        @Override
        public int fee(int amount) {
            return amount * 15 / 1000;                // 1.5%
        }

        @Override
        protected String callApi(int total) {
            return "RAZORPAY-OK-" + total;
        }

        @Override
        public String refund(String paymentId) {
            return "refunded " + paymentId;
        }
    }

    /** D: a fake for tests. PaymentService accepts it because it depends on the interface. */
    static class FakeGateway implements PaymentGateway {
        @Override
        public String name() {
            return "FAKE";
        }

        @Override
        public int fee(int amount) {
            return 0;
        }

        @Override
        public String pay(int amount) {
            return "FAKE-OK-" + amount;
        }
    }

    /** Depends only on PaymentGateway, never on PayU or Setu directly (D). No if-else per gateway (O). */
    static class PaymentService {
        private final Map<String, PaymentGateway> gateways = new LinkedHashMap<>();

        PaymentService(List<PaymentGateway> all) {    // in Spring, the container would inject this list
            for (PaymentGateway gateway : all) {
                gateways.put(gateway.name(), gateway);
            }
        }

        String pay(String gatewayName, int amount) {
            return gateways.get(gatewayName).pay(amount);
        }
    }

    // =========================================================================
    // Step 3: Liskov
    // =========================================================================
    static class Account {
        int balance;

        Account(int balance) {
            this.balance = balance;
        }

        void withdraw(int amount) {
            balance -= amount;
        }
    }

    /** Breaks Liskov on purpose: it can't do what its parent promises. */
    static class FixedDepositAccount extends Account {
        FixedDepositAccount(int balance) {
            super(balance);
        }

        @Override
        void withdraw(int amount) {
            throw new UnsupportedOperationException("FD is locked until maturity");
        }
    }

    /** Written for ANY Account, so every subclass must be able to withdraw. */
    static void payBill(Account from, int amount) {
        from.withdraw(amount);
    }

    // =========================================================================
    // Step 7: immutable vs leaky
    // =========================================================================

    /** NOT immutable: it keeps the caller's list and hands it out. */
    static final class LeakyPaymentRequest {
        private final List<String> tags;              // final only stops reassigning, not changing the list

        LeakyPaymentRequest(List<String> tags) {
            this.tags = tags;
        }

        List<String> tags() {
            return tags;
        }
    }

    /** Immutable: final class, private final fields, no setters, defensive copy. */
    static final class PaymentRequest {
        private final String txnId;
        private final int amount;
        private final List<String> tags;

        PaymentRequest(String txnId, int amount, List<String> tags) {
            this.txnId = txnId;
            this.amount = amount;
            this.tags = List.copyOf(tags);            // our own unmodifiable copy
        }

        int amount() {
            return amount;
        }

        List<String> tags() {
            return tags;                              // safe to hand out: nobody can modify it
        }

        PaymentRequest withAmount(int newAmount) {
            return new PaymentRequest(txnId, newAmount, tags);   // a "change" = a new object
        }
    }

    static void step(String text) {
        System.out.println();
        System.out.println("=== " + text + " ===");
    }
}
