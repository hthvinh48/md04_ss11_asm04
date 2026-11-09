package com.example.inventoryservice.service;

import com.example.inventoryservice.entity.Medicine;
import com.example.inventoryservice.repository.MedicineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SellService {

    private final RedissonClient redissonClient;
    private final MedicineRepository medicineRepository;
    private final TransactionTemplate transactionTemplate;

    public String sellMedicine(String id, int quantity) {
        RLock lock = redissonClient.getLock("lock:medicine:" + id);
        boolean locked = false;
        try {
            // chờ tối đa 3s để lấy lock, giữ lock tối đa 5s
            locked = lock.tryLock(3, 5, TimeUnit.SECONDS);
            if (!locked) {
                return "Hệ thống đang bận, vui lòng thử lại";
            }

            // transaction nằm TRONG lock, commit xong mới nhả lock
            return transactionTemplate.execute(status -> {
                Medicine m = medicineRepository.findById(id).orElse(null);
                if (m == null) return "Không tìm thấy thuốc " + id;
                if (m.getStock() < quantity) return "Hết hàng";
                m.setStock(m.getStock() - quantity);
                medicineRepository.save(m);
                return "Mua thành công, còn lại " + m.getStock();
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Bị gián đoạn";
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}



