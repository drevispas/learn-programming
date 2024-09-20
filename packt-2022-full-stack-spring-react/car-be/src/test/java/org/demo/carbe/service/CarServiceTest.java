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
        car1 = Car.builder().brand("Toyota").model("Camry").color("Black").manufacturingYear(2020).price(35000).build();
        car2 = Car.builder().brand("Honda").model("Civic").color("White").manufacturingYear(2021).price(30000).build();
        car3 = Car.builder().brand("Ford").model("Fusion").color("Silver").manufacturingYear(2019).price(32000).build();
        car4 = Car.builder().brand("Chevrolet").model("Malibu").color("Red").manufacturingYear(2020).price(33000).build();
//        owners = List.of(owner1, owner2);
//        carsOfOwner1 = List.of(car1, car2);
//        carsOfOwner2 = List.of(car3, car4);
    }

    @AfterEach
    public void tearDown() {
        logger.warning("Deleting all data");
        carRepository.deleteAll();
        ownerRepository.deleteAll();
    }

    /*
    [Bidirectional @OneToMany and @ManyToOne relationship]
    ---
    @Builder.Default
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner", fetch = FetchType.EAGER|LAZY)
    private List<Car> cars = new ArrayList<>();
    ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private Owner owner;
    ---
    "insert into owner (first_name,last_name,id) values (?,?,default)"
    "insert into car (brand,color,manufacturing_year,model,owner_id,price,register_number,id) values (?,?,?,?,?,?,?,default)" * 2

    [Unidirectional @OneToMany relationship]
    ---
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Car> cars = new ArrayList<>();
    ---
    // private Owner owner; // No parent reference in Car class
    ---
    "insert into owner (first_name,last_name,id) values (?,?,default)"
    "insert into car (brand,color,manufacturing_year,model,price,register_number,id) values (?,?,?,?,?,?,default)" * 2
    "insert into owner_cars (owner_id,cars_id) values (?,?)" * 2

    [Unidirectional @ManyToOne with @JoinColumn relationship]
    ---
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private List<Car> cars = new ArrayList<>();
    ---
    // private Owner owner; // No parent reference in Car class
    ---
    "insert into owner (first_name,last_name,id) values (?,?,default)"
    "insert into car (brand,color,manufacturing_year,model,price,register_number,id) values (?,?,?,?,?,?,default)" * 2
    "update car set owner_id=? where id=?" * 2
     */
    @Test
    public void testInsertOwner() {
        owner1.getCars().add(car1);
        owner1.getCars().add(car2);
        logger.warning("Saving data");
        ownerRepository.save(owner1);
    }
}
