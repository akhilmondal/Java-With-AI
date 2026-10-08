package Practice;

import java.util.Arrays;
import java.util.Comparator;
import java.util.IntSummaryStatistics;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/*
 * Every stream operation, one by one, with a small example.
 *
 * A stream = source -> intermediate operations (0 or more) -> ONE terminal operation
 *
 *   INTERMEDIATE: give back a new Stream. They are LAZY: nothing runs until a terminal op.
 *     filter, map, mapToInt / mapToObj / boxed, flatMap, distinct, sorted, peek,
 *     limit, skip, takeWhile, dropWhile
 *
 *   TERMINAL: give back a result (a number, a list, an Optional, boolean) or nothing.
 *     They START the work, and after them the stream is used up.
 *     forEach, forEachOrdered, collect, toList, toArray, reduce, count, min, max,
 *     sum / average / summaryStatistics, anyMatch / allMatch / noneMatch,
 *     findFirst / findAny, iterator
 *
 * HOW TO RUN   java 01-java-core/Practice/Streams.java   (or click "Run" above main)
 * The "// ->" comment under each line is the real output.
 */
public class Streams {
    public static void main(String[] args) {

        // ===================== Your warm-up (your own code) =====================
        int[] array = {1,2,3,3,4,5,0,6,7,4,43,43,2,4,5,5};
        int sum = Arrays.stream(array).filter(n -> n % 2 == 0).sum();
        System.out.println(sum);
        // -> 22   (2+4+0+6+4+2+4)

        List<Integer> list = Arrays.asList(1,2,3,3,4,5,0,6,7,4,43,43,2,4,5,5);
        List<Integer> filteredList = list.stream().filter(n -> n%2==0).collect(Collectors.toList());
        System.out.println(filteredList);
        // -> [2, 4, 0, 6, 4, 2, 4]

        List<Integer> newList = filteredList.stream().map(n-> n/2).distinct().sorted().collect(Collectors.toList());
        System.out.println(newList);
        // -> [0, 1, 2, 3]

        // The data used in all the examples below
        List<Integer> nums  = List.of(5, 3, 8, 3, 1, 8, 10);
        List<String>  names = List.of("Rahul", "Priya", "Amit", "Neha", "Karan");

        // =========================================================================
        // PART A: INTERMEDIATE OPERATIONS (return a Stream, lazy)
        // =========================================================================
        title("PART A: INTERMEDIATE");

        // 1. filter(condition): keep only the elements that pass the test
        System.out.println("filter    : " + nums.stream().filter(n -> n > 4).toList());
        // -> filter    : [5, 8, 8, 10]

        // 2. map(function): change every element into something else (one in, one out)
        System.out.println("map       : " + nums.stream().map(n -> n * 2).toList());
        // -> map       : [10, 6, 16, 6, 2, 16, 20]
        System.out.println("map (len) : " + names.stream().map(String::length).toList());
        // -> map (len) : [5, 5, 4, 4, 5]

        // 3. mapToInt: Integer -> int, so you get sum(), average(), max() for numbers
        System.out.println("mapToInt  : " + nums.stream().mapToInt(Integer::intValue).sum());
        // -> mapToInt  : 38

        //    mapToObj: int -> object.   boxed: int -> Integer (to collect into a List)
        System.out.println("mapToObj  : " + IntStream.rangeClosed(1, 3).mapToObj(i -> "TXN" + i).toList());
        // -> mapToObj  : [TXN1, TXN2, TXN3]
        System.out.println("boxed     : " + IntStream.of(4, 5, 6).boxed().toList());
        // -> boxed     : [4, 5, 6]

        // 4. flatMap: each element gives MANY elements, and they are flattened into one stream
        List<List<String>> skills = List.of(List.of("Java", "SQL"), List.of("Angular"), List.of("Java", "Spring"));
        System.out.println("flatMap   : " + skills.stream().flatMap(List::stream).toList());
        // -> flatMap   : [Java, SQL, Angular, Java, Spring]
        System.out.println("flatMap ch: " + Stream.of("ab", "cd").flatMap(s -> s.chars().mapToObj(c -> (char) c)).toList());
        // -> flatMap ch: [a, b, c, d]

        // 5. distinct: remove duplicates (uses equals/hashCode, keeps first-seen order)
        System.out.println("distinct  : " + nums.stream().distinct().toList());
        // -> distinct  : [5, 3, 8, 1, 10]

        // 6. sorted(): natural order.  sorted(comparator): your own order
        System.out.println("sorted    : " + nums.stream().sorted().toList());
        // -> sorted    : [1, 3, 3, 5, 8, 8, 10]
        System.out.println("sorted rev: " + nums.stream().sorted(Comparator.reverseOrder()).toList());
        // -> sorted rev: [10, 8, 8, 5, 3, 3, 1]
        System.out.println("sorted len: " + names.stream()
                .sorted(Comparator.comparing(String::length).thenComparing(Comparator.naturalOrder()))
                .toList());
        // -> sorted len: [Amit, Neha, Karan, Priya, Rahul]

        // 7. peek: look at each element as it passes (for debugging), does not change it
        List<Integer> peeked = nums.stream()
                .filter(n -> n > 7)
                .peek(n -> System.out.println("  peek saw " + n))
                .toList();
        System.out.println("peek      : " + peeked);
        // ->   peek saw 8
        // ->   peek saw 8
        // ->   peek saw 10
        // -> peek      : [8, 8, 10]

        // 8. limit(n): take only the first n
        System.out.println("limit(3)  : " + nums.stream().limit(3).toList());
        // -> limit(3)  : [5, 3, 8]

        // 9. skip(n): throw away the first n
        System.out.println("skip(3)   : " + nums.stream().skip(3).toList());
        // -> skip(3)   : [3, 1, 8, 10]

        // 10. takeWhile (Java 9): take from the start WHILE the condition is true, stop at the first false
        System.out.println("takeWhile : " + nums.stream().takeWhile(n -> n > 2).toList());
        // -> takeWhile : [5, 3, 8, 3]

        // 11. dropWhile (Java 9): drop from the start WHILE true, keep everything after
        System.out.println("dropWhile : " + nums.stream().dropWhile(n -> n > 2).toList());
        // -> dropWhile : [1, 8, 10]

        // LAZY proof: no terminal operation, so the filter never runs and nothing prints
        nums.stream().filter(n -> {
            System.out.println("  you will never see this");
            return true;
        });
        System.out.println("lazy      : no terminal op, so filter did not run");
        // -> lazy      : no terminal op, so filter did not run

        // =========================================================================
        // PART B: TERMINAL OPERATIONS (start the work, give a result)
        // =========================================================================
        title("PART B: TERMINAL");

        // 1. forEach: do something with each element, returns nothing
        System.out.print("forEach        : ");
        names.stream().forEach(n -> System.out.print(n + " "));
        System.out.println();
        // -> forEach        : Rahul Priya Amit Neha Karan

        // 2. forEachOrdered: like forEach, but keeps order even on a parallel stream
        System.out.print("forEachOrdered : ");
        names.parallelStream().forEachOrdered(n -> System.out.print(n + " "));
        System.out.println();
        // -> forEachOrdered : Rahul Priya Amit Neha Karan

        // 3. collect: put the results into a collection (the Collectors class has the recipes)
        List<Integer> asList = nums.stream().collect(Collectors.toList());
        Set<Integer> asSet = nums.stream().collect(Collectors.toSet());
        String joined = names.stream().collect(Collectors.joining(", ", "[", "]"));
        Map<String, Integer> nameToLength = names.stream().collect(Collectors.toMap(n -> n, String::length));
        Map<Integer, List<String>> byLength = names.stream().collect(Collectors.groupingBy(String::length));
        Map<Boolean, List<Integer>> evenOdd = nums.stream().collect(Collectors.partitioningBy(n -> n % 2 == 0));
        Map<Integer, Long> countEach = nums.stream().collect(Collectors.groupingBy(n -> n, Collectors.counting()));
        System.out.println("toList         : " + asList);
        // -> toList         : [5, 3, 8, 3, 1, 8, 10]
        System.out.println("toSet          : " + asSet + "   (no duplicates, order not promised)");
        // -> toSet          : [1, 3, 5, 8, 10]   (no duplicates, order not promised)
        System.out.println("joining        : " + joined);
        // -> joining        : [Rahul, Priya, Amit, Neha, Karan]
        System.out.println("toMap          : " + nameToLength + "   (HashMap, order not promised)");
        // -> toMap          : {Rahul=5, Priya=5, Neha=4, Karan=5, Amit=4}   (HashMap, order not promised)
        System.out.println("groupingBy     : " + byLength);
        // -> groupingBy     : {4=[Amit, Neha], 5=[Rahul, Priya, Karan]}
        System.out.println("partitioningBy : " + evenOdd);
        // -> partitioningBy : {false=[5, 3, 3, 1], true=[8, 8, 10]}
        System.out.println("count each     : " + countEach);
        // -> count each     : {1=1, 3=2, 5=1, 8=2, 10=1}

        // 4. toList() (Java 16): shorter than collect(Collectors.toList()), but the list can't be changed
        List<String> shortList = names.stream().filter(n -> n.startsWith("A")).toList();
        System.out.println("toList()       : " + shortList);
        // -> toList()       : [Amit]

        // 5. toArray: get an array instead of a list
        String[] nameArray = names.stream().map(String::toUpperCase).toArray(String[]::new);
        System.out.println("toArray        : " + Arrays.toString(nameArray));
        // -> toArray        : [RAHUL, PRIYA, AMIT, NEHA, KARAN]

        // 6. reduce: combine all elements into ONE value
        int total = nums.stream().reduce(0, Integer::sum);              // start value 0, then add
        Optional<Integer> biggest = nums.stream().reduce(Integer::max); // no start value -> Optional
        System.out.println("reduce sum     : " + total);
        // -> reduce sum     : 38
        System.out.println("reduce max     : " + biggest.get());
        // -> reduce max     : 10

        // 7. count: how many elements
        System.out.println("count          : " + nums.stream().filter(n -> n > 4).count());
        // -> count          : 4

        // 8. min / max (with a comparator): give an Optional, because the stream may be empty
        System.out.println("min            : " + nums.stream().min(Integer::compare).get());
        // -> min            : 1
        System.out.println("max            : " + nums.stream().max(Integer::compare).get());
        // -> max            : 10
        System.out.println("longest name   : " + names.stream().max(Comparator.comparing(String::length)).get());
        // -> longest name   : Rahul

        // 9. sum / average / summaryStatistics: only on IntStream, LongStream, DoubleStream
        System.out.println("sum            : " + nums.stream().mapToInt(n -> n).sum());
        // -> sum            : 38
        System.out.println("average        : " + nums.stream().mapToInt(n -> n).average().getAsDouble());
        // -> average        : 5.428571428571429
        IntSummaryStatistics stats = nums.stream().mapToInt(n -> n).summaryStatistics();
        System.out.println("statistics     : " + stats);
        // -> statistics     : IntSummaryStatistics{count=7, sum=38, min=1, average=5.428571, max=10}

        // 10. anyMatch / allMatch / noneMatch: a yes/no answer, they stop early
        System.out.println("anyMatch(>9)   : " + nums.stream().anyMatch(n -> n > 9));
        // -> anyMatch(>9)   : true
        System.out.println("allMatch(>0)   : " + nums.stream().allMatch(n -> n > 0));
        // -> allMatch(>0)   : true
        System.out.println("noneMatch(<0)  : " + nums.stream().noneMatch(n -> n < 0));
        // -> noneMatch(<0)  : true

        // 11. findFirst / findAny: give an Optional with one element
        System.out.println("findFirst(>5)  : " + nums.stream().filter(n -> n > 5).findFirst().get());
        // -> findFirst(>5)  : 8
        System.out.println("findAny(>5)    : " + nums.stream().filter(n -> n > 5).findAny().get()
                + "   (any match; on a parallel stream it can differ)");
        // -> findAny(>5)    : 8   (any match; on a parallel stream it can differ)
        System.out.println("findFirst(>99) : " + nums.stream().filter(n -> n > 99).findFirst().orElse(-1));
        // -> findFirst(>99) : -1

        // 12. iterator: get an old-style Iterator (rarely used)
        Iterator<String> it = names.stream().limit(2).iterator();
        System.out.print("iterator       : ");
        while (it.hasNext()) System.out.print(it.next() + " ");
        System.out.println();
        // -> iterator       : Rahul Priya

        // A stream can be used ONLY ONCE. After a terminal op, using it again throws an exception.
        Stream<Integer> once = nums.stream();
        once.count();
        try {
            once.count();
        } catch (IllegalStateException e) {
            System.out.println("used twice     : " + e.getMessage());
        }
        // -> used twice     : stream has already been operated upon or closed
    }

    static void title(String text) {
        System.out.println();
        System.out.println("=== " + text + " ===");
    }
}
