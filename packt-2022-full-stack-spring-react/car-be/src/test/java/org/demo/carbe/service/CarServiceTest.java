package org.demo.carbe.service;

import org.demo.carbe.domain.Car;
import org.demo.carbe.domain.Owner;
import org.demo.carbe.repository.CarRepository;
import org.demo.carbe.repository.OwnerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.logging.Logger;

@SpringBootTest
class CarServiceTest {

    Logger logger = Logger.getLogger(CarServiceTest.class.getName());

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private CarService carService;

    private Owner owner1;
    private Owner owner2;
    private List<Owner> owners;

    private Car car1;
    private Car car2;
    private Car car3;
    private Car car4;
    private List<Car> carsOfOwner1;
    private List<Car> carsOfOwner2;

    @BeforeEach
    public void setUp() {
        owner1 = Owner.builder().firstName("John").lastName("Doe").build();
        owner2 = Owner.builder().firstName("Mary").lastName("Jane").build();
        owners = List.of(owner1, owner2);
        car1 = Car.builder().brand("Toyota").model("Camry").color("Black").manufacturingYear(2020).price(35000).owner(owner1).build();
        car2 = Car.builder().brand("Honda").model("Civic").color("White").manufacturingYear(2021).price(30000).owner(owner1).build();
        carsOfOwner1 = List.of(car1, car2);
        car3 = Car.builder().brand("Ford").model("Fusion").color("Silver").manufacturingYear(2019).price(32000).owner(owner2).build();
        car4 = Car.builder().brand("Chevrolet").model("Malibu").color("Red").manufacturingYear(2020).price(33000).owner(owner2).build();
        carsOfOwner2 = List.of(car3, car4);
    }

    @AfterEach
    public void tearDown() {
        logger.warning("Deleting all data");
        carRepository.deleteAll();
        ownerRepository.deleteAll();
    }

    /*
    @Builder.Default
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner", fetch = FetchType.EAGER)
    private List<Car> cars = new ArrayList<>();
    ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private Owner owner;
    ---
    insert into owner (first_name,last_name,id) values (?,?,default)
    insert into car (brand,color,manufacturing_year,model,owner_id,price,register_number,id) values (?,?,?,?,?,?,?,default)
     */
    @Test
    public void testBiidirectionalOneToManyAndManyToOne() {
//        owner1.getCars().add(Car.builder().brand("Toyota").model("Camry").color("Black").manufacturingYear(2020).price(35000).owner(owner1).build());
//        owner1.getCars().add(car2);
        owner1.getCars().add(car1);
        logger.warning("Saving data");
        ownerRepository.save(owner1);
    }
}
