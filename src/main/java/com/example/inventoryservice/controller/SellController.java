package com.example.inventoryservice.controller;

import com.example.inventoryservice.service.SellService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@RestController
@RequestMapping("/api/sell")
@RequiredArgsConstructor
public class SellController {

    private final SellService sellService;

    @PostMapping("/{id}")
    public String sell(@PathVariable String id, @RequestParam int quantity) {
        return sellService.sellMedicine(id, quantity);
    }

    // Mô phỏng 2 nhân viên bấm cùng lúc
    @PostMapping("/{id}/simulate")
    public List<String> simulate(@PathVariable String id) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<String>> futures = new ArrayList<>();
        for (String name : List.of("Nhân viên A", "Nhân viên B")) {
            futures.add(pool.submit(() -> {
                start.await();                       // chờ hiệu lệnh để chạy cùng lúc
                String result = sellService.sellMedicine(id, 1);
                return name + ": " + result;
            }));
        }
        start.countDown();                           // xuất phát

        List<String> results = new ArrayList<>();
        for (Future<String> f : futures) results.add(f.get());
        pool.shutdown();
        return results;
    }
}
