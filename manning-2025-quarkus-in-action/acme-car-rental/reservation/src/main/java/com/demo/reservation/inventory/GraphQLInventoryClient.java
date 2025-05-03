package com.demo.reservation.inventory;

import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;
import java.util.List;
import org.eclipse.microprofile.graphql.Query;

@GraphQLClientApi(configKey = "inventory")
public interface GraphQLInventoryClient extends InventoryClient {

    @Query("cars")
    List<Car> listAllCars();
}
