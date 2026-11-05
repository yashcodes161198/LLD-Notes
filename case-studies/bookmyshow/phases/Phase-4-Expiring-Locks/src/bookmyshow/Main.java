package bookmyshow;

import bookmyshow.model.*;
import bookmyshow.enums.*;
import bookmyshow.repository.*;
import bookmyshow.service.*;
import bookmyshow.strategy.payment.*;
import bookmyshow.factory.*;
import bookmyshow.strategy.locking.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class Main {
    public static void main(String[] args) throws Exception {
        AtomicLong clock = new AtomicLong(1_000);
        try (InMemoryLockProvider locks = new InMemoryLockProvider(clock::get)) {
            String key = "show1:s1-6";
            System.out.println("t=0 user4 acquires: "
                + locks.tryLock(key, "user4", 5_000));
            System.out.println("t=0 user5 acquires: "
                + locks.tryLock(key, "user5", 5_000));
            clock.addAndGet(5_000);
            System.out.println("t=5s expired: " + locks.isLockExpired(key));
            System.out.println("t=5s user5 acquires: "
                + locks.tryLock(key, "user5", 5_000));
            System.out.println("Old owner still owns: "
                + locks.isLockedBy(key, "user4"));
            System.out.println("Same seat, another show: "
                + locks.tryLock("show2:s1-6", "user4", 5_000));
        }
    }

}
