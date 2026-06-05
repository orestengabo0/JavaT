package com.spring.JavaT.notification;

import com.spring.JavaT.notification.dto.NotificationDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface NotificationMapper {

    @Mapping(source = "bill.id", target = "billId")
    NotificationDto toDto(Notification notification);
}
