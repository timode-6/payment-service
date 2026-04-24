package com.example.payment_service.repository;

import com.example.payment_service.model.Payment;
import com.example.payment_service.model.PaymentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.mongodb.repository.Query;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {

    @Query("{ '_id': ?0, 'deleted': false }")
    Optional<Payment> findById(String id);

    @Query("{ 'order_id': ?0, 'deleted': false }")
    boolean existsByOrderId(String orderId);

    @Query("{ 'user_id': ?0, 'deleted': false }")
    List<Payment> findByUserId(String userId);

    @Query("{ 'order_id': ?0, 'deleted': false }")
    Optional<Payment> findByOrderId(String orderId);

    @Query("{ 'status': ?0, 'deleted': false }")
    List<Payment> findByStatus(PaymentStatus status);

    @Query("{ 'user_id': ?0, 'status': ?1, 'deleted': false }")
    List<Payment> findByUserIdAndStatus(String userId, PaymentStatus status);
}