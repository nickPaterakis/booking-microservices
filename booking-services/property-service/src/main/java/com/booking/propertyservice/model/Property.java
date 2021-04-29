package com.booking.propertyservice.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Entity
@Table(name = "properties")
public class Property extends BaseEntity{

    @Column(name = "title")
    private String title;

    @ManyToOne
    @JoinColumn(name = "property_type_id")
    private PropertyType propertyType;

    @ManyToOne
    @JoinColumn(name = "guest_space_id")
    private GuestSpace guestSpace;

    @Column(name = "max_guest_number")
    private Integer maxGuestNumber;

    @Column(name = "bedroom_number")
    private Integer bedroomNumber;

    @Column(name = "bath_number")
    private  Integer bathNumber;

    @Column(name = "price_per_night")
    private Float pricePerNight;

    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "owner", columnDefinition = "BINARY(16)")
    private UUID owner;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "property_amenities",
            joinColumns = @JoinColumn(name = "property_id"),
            inverseJoinColumns = @JoinColumn(name = "amenity_id")
    )
    Set<Amenity> amenities;

    @ManyToOne
    @JoinColumn(name = "country_id")
    private Country country;

    @OneToMany(mappedBy = "property")
    private Set<Reservation> reservations;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "property")
    private Set<Image> images;

}
