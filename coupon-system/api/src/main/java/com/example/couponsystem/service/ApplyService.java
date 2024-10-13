package com.example.couponsystem.service;

import com.example.couponsystem.domain.Coupon;
import com.example.couponsystem.producer.CouponCreateProducer;
import com.example.couponsystem.repository.AppliedUserRepository;
import com.example.couponsystem.repository.CouponCountRepository;
import com.example.couponsystem.repository.CouponRepository;
import org.springframework.stereotype.Service;

@Service
public class ApplyService {
    private final CouponRepository couponRepository;
    private final CouponCountRepository couponCountRepository;
    private final CouponCreateProducer couponCreateProducer;
    private final AppliedUserRepository appliedUserRepository;

    public ApplyService(CouponRepository couponRepository,
                        CouponCountRepository couponCountRepository,
                        CouponCreateProducer couponCreateProducer,
                        AppliedUserRepository appliedUserRepository) {
        this.couponRepository = couponRepository;
        this.couponCountRepository = couponCountRepository;
        this.couponCreateProducer = couponCreateProducer;
        this.appliedUserRepository = appliedUserRepository;
    }

    public void apply(Long userId) {
        Long apply = appliedUserRepository.add(userId);
        if(apply != 1) {
            return;
        }

        long count = couponCountRepository.increment();

        if (count > 100) {
            return;
        }

        couponCreateProducer.create(userId);
        //couponRepository.save(new Coupon(userId));
    }
}
