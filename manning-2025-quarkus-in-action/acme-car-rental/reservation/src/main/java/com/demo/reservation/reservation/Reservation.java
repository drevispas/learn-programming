package com.demo.reservation.reservation;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.logging.Log;
import jakarta.persistence.Entity;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
//@Data
//@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Reservation extends PanacheEntity {

//    Long id;
    public String userId;
    public Long carId;
    public LocalDate startDate;
    public LocalDate endDate;

    public boolean isReserved(LocalDate startDate, LocalDate endDate) {
        Log.infof("Checking if reservation %s is reserved between %s and %s", this, startDate, endDate);
        return (!(this.startDate.isAfter(endDate) || this.endDate.isBefore(startDate)));
    }

//    public Reservation withId(Long id) {
//
//        return new Reservation(id, userId, this.carId, this.startDate, this.endDate);
//    }

    public Reservation withUserId(String userId) {
        return new Reservation(userId, this.carId, this.startDate, this.endDate);
    }
}
