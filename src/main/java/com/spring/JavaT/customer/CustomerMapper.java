package com.spring.JavaT.customer;

import com.spring.JavaT.customer.dto.CustomerDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CustomerMapper {

    @Mapping(source = "user.id", target = "userId")
    CustomerDto toDto(Customer customer);
}
