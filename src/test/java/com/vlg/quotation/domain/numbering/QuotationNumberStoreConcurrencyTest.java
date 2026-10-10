package com.vlg.quotation.domain.numbering;

import static org.assertj.core.api.Assertions.assertThat;

import com.vlg.quotation.domain.numbering.QuotationNumberService.AllocatedNumber;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Contract proof for the owning store: concurrent allocate-next calls must
 * produce distinct numbers with none lost. The Android SQLite implementation
 * must satisfy the same property via a transaction around check-and-reserve.
 */
class QuotationNumberStoreConcurrencyTest {

    private static final int THREAD_COUNT = 32;
    private static final int ALLOCATIONS_PER_THREAD = 25;

    @Test
    void shouldAllocateDistinctNumbersUnderContention() throws Exception {
        QuotationNumberService numberService = new QuotationNumberService();
        InMemoryQuotationNumberStore numberStore = new InMemoryQuotationNumberStore();
        QuotationNumberConfiguration configuration = new QuotationNumberConfiguration("VLG", "2025", 1, 3);

        ExecutorService allocationPool = Executors.newFixedThreadPool(THREAD_COUNT);
        try {
            List<Callable<List<String>>> allocationTasks = new ArrayList<>();
            for (int taskIndex = 0; taskIndex < THREAD_COUNT; taskIndex++) {
                allocationTasks.add(() -> {
                    List<String> threadAllocations = new ArrayList<>();
                    for (int allocationIndex = 0; allocationIndex < ALLOCATIONS_PER_THREAD; allocationIndex++) {
                        AllocatedNumber allocatedNumber = numberService.allocateNext(configuration, numberStore);
                        threadAllocations.add(allocatedNumber.formattedNumber());
                    }
                    return threadAllocations;
                });
            }
            List<String> allAllocations = new ArrayList<>();
            for (Future<List<String>> completedTask : allocationPool.invokeAll(allocationTasks, 60, TimeUnit.SECONDS)) {
                allAllocations.addAll(completedTask.get(10, TimeUnit.SECONDS));
            }
            assertThat(allAllocations).hasSize(THREAD_COUNT * ALLOCATIONS_PER_THREAD);
            assertThat(new HashSet<>(allAllocations)).hasSize(THREAD_COUNT * ALLOCATIONS_PER_THREAD);
            assertThat(numberStore.usedCount()).isEqualTo(THREAD_COUNT * ALLOCATIONS_PER_THREAD);
        } finally {
            allocationPool.shutdownNow();
        }
    }
}
