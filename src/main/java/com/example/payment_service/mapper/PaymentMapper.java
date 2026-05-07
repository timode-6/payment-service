package com.example.payment_service.mapper;

import com.example.payment_service.dto.requests.CreatePaymentRequest;
import com.example.payment_service.dto.requests.PaymentDto;
import com.example.payment_service.model.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;


import java.util.List;
import java.time.Instant;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface PaymentMapper {


    PaymentDto toDto(Payment payment);

    List<PaymentDto> toDtoList(List<Payment> payments);


    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "timestamp", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Payment toEntity(PaymentDto dto);


    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "status",    ignore = true)
    @Mapping(target = "timestamp", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted",   constant = "false")
    Payment fromCreateRequest(CreatePaymentRequest request);


    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "orderId",   ignore = true)
    @Mapping(target = "userId",    ignore = true)
    @Mapping(target = "timestamp", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void applyUpdate(PaymentDto source, @MappingTarget Payment target);

    default void applySoftDelete(Payment payment) {
        payment.setDeleted(true);
        payment.setDeletedAt(Instant.now());
    }
}