package com.booking.propertyservice.service;

import com.booking.commondomain.dto.property.*;
import com.booking.commondomain.dto.user.BookingUser;
import com.booking.bookingutils.exception.NotFoundException;
import com.booking.propertyservice.integration.reservationservice.ReservationServiceIntegration;
import com.booking.propertyservice.integration.userservice.UserServiceIntegration;
import com.booking.propertyservice.mapper.PropertyMapper;
import com.booking.propertyservice.model.Property;
import com.booking.propertyservice.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyServiceImpl implements PropertyService {

    private final PropertyRepository propertyRepository;
    private final ReservationServiceIntegration reservationServiceIntegration;
    private final UserServiceIntegration userServiceIntegration;
    private final Scheduler scheduler;

    @Override
    @Transactional(readOnly = true)
    public Mono<PageProperties> searchProperties(
            String location,
            LocalDate checkIn,
            LocalDate checkOut,
            int guestNumber,
            int currentPage
    ) {
        log.info("Search properties by location: {}", location);
        Pageable pageable;
        pageable = PageRequest.of(currentPage,5);
        PageProperties result = new PageProperties();
        List<Long> propertyIds = new ArrayList<>();

        return asyncMono(() ->
                reservationServiceIntegration.getPropertyIds(location, checkIn, checkOut)
                .collectList()
                .map(longs -> {
                    if (propertyIds.isEmpty())
                        propertyIds.add(0L);
                    propertyIds.addAll(longs);
                    result.setTotalElements(propertyRepository.count(propertyIds, location, guestNumber));
                    return result.getTotalElements();
                })
                .flatMapMany(el ->
                        Flux.fromIterable(propertyRepository.searchProperties(propertyIds, location, guestNumber, pageable)
                        .stream()
                        .map(PropertyMapper::toPropertyDto)
                        .collect(Collectors.toList())))
                .collectList()
                .map(data -> {
                    result.setProperties(data);
                    return result;
                }));
    }

    @Override
    public Mono<PageProperties> getProperties(@AuthenticationPrincipal BookingUser user, int currentPage) {
        log.info("Get properties by user id: {}", UUID.fromString(user.getId()));
        Pageable pageable;
        pageable = PageRequest.of(currentPage,5);
        PageProperties result = new PageProperties();
        return asyncMono(() -> Mono.just(
                propertyRepository.count(user.getId()))
                .map(totalElements -> {
                    result.setTotalElements(totalElements);
                    return totalElements;
                })
                .flatMapMany(el -> Flux.fromIterable(propertyRepository.findByOwner(user.getId(), pageable)
                        .stream()
                        .map(PropertyMapper::toPropertyDto)
                        .collect(Collectors.toList())))
                .collectList()
                .map(data -> {
                    result.setProperties(data);
                    return result;
                }));
    }

    @Override
    public Mono<PropertyAggregate> getProperty(Long propertyId) {
        log.info("Get property by id: {}", propertyId);
        PropertyAggregate propertyAggregate = new PropertyAggregate();
        return asyncMono(() -> Mono.just(propertyRepository.findById(propertyId)
                        .orElseThrow(() -> new NotFoundException(String.format("Property with id %d not found ", propertyId)))
                ).map(property -> {
                    PropertyDetailsDto propertyDetailsDto = PropertyMapper.toPropertyDetailsDto(property);
                    propertyAggregate
                            .setId(propertyDetailsDto.getId())
                            .setTitle(propertyDetailsDto.getTitle())
                            .setPropertyType(propertyDetailsDto.getPropertyType().getName())
                            .setGuestSpace(propertyDetailsDto.getGuestSpace().getName())
                            .setMaxGuestNumber(propertyDetailsDto.getMaxGuestNumber())
                            .setBedroomNumber(propertyDetailsDto.getBedroomNumber())
                            .setBathNumber(propertyDetailsDto.getBathNumber())
                            .setPricePerNight(propertyDetailsDto.getPricePerNight())
                            .setAddress(new AddressDto(
                                    propertyDetailsDto.getCity(),
                                    propertyDetailsDto.getCountry().getName(),
                                    propertyDetailsDto.getPostCode(),
                                    propertyDetailsDto.getStreetName(),
                                    propertyDetailsDto.getStreetNumber()
                            ))
                            .setDescription(propertyDetailsDto.getDescription())
                            .setImages(propertyDetailsDto.getImages())
                            .setAmenities(propertyDetailsDto.getAmenities())
                            .setOwnerId(propertyDetailsDto.getOwnerId());
                    return propertyAggregate;
                })
                .flatMap(property -> userServiceIntegration.getUserById(property.getOwnerId()))
                .map(userDto -> {
                    propertyAggregate
                            .setOwnerFirstName(userDto.getFirstName())
                            .setOwnerLastName(userDto.getLastName())
                            .setOwnerImage(userDto.getImage());
                    return propertyAggregate;
                })
                );
    }

    @Override
    public Mono<Void> createProperty(PropertyDetailsDto propertyDetailsDto) {
        log.info("Create Property By User: {}", propertyDetailsDto.getOwnerId());
        Property property = PropertyMapper.toProperty(propertyDetailsDto);
        property.getPropertyType().addProperty(property);
        property.getGuestSpace().addProperty(property);
        property.getAddress().getCountry().addAddress(property.getAddress());
        propertyRepository.save(property);

        return Mono.empty();
    }

    @Override
    public void deleteProperty(Long id) {
        log.info("deleteProperty");
        propertyRepository.deleteById(id);
        reservationServiceIntegration.deleteAllReservationsByPropertyId(id);
    }

    @Override
    public Mono<PropertyReservationDataDto> getPropertyById(Long propertyId) {
        log.info("Get properties by id: {}", propertyId);
        return asyncMono(() -> Mono.just(propertyRepository.getPropertyById(propertyId)).map(PropertyMapper::toPropertyReservationDataDto));
    }

    private <T> Flux<T> asyncFlux(Supplier<Publisher<T>> publisherSupplier) {
        return Flux.defer(publisherSupplier).subscribeOn(scheduler);
    }

    private <T> Mono<T> asyncMono(Supplier<Mono<T>> publisherSupplier) {
        return Mono.defer(publisherSupplier).subscribeOn(scheduler);
    }
}
