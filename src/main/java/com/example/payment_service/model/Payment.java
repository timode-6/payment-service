package com.example.payment_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Data
@Builder
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "payments")
@CompoundIndex(name = "idx_payments_user_status",    def = "{'user_id': 1, 'status': 1}")
@CompoundIndex(name = "idx_payments_user_timestamp", def = "{'user_id': 1, 'timestamp': -1}")
public class Payment {

    @Id
    private String id;

    @Indexed(name = "idx_payments_order_id", unique = true)
    @Field("order_id")
    private String orderId;

    @Indexed(name = "idx_payments_user_id")
    @Field("user_id")
    private String userId;

    @Indexed(name = "idx_payments_status")
    private PaymentStatus status;

    @CreatedDate
    @Indexed(name = "idx_payments_timestamp")
    private Instant timestamp;

    @Field("payment_amount")
    private Long paymentAmount;

    @LastModifiedDate
    @Field("updated_at")
    private Instant updatedAt;

    @Field("deleted")
    @Builder.Default
    private boolean deleted = false;

    @Field("deleted_at")
    private Instant deletedAt;
}