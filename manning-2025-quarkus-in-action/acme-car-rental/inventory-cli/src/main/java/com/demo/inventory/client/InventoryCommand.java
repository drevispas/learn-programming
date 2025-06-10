package com.demo.inventory.client;

import com.demo.inventory.model.InsertCarRequest;
import com.demo.inventory.model.InventoryService;
import com.demo.inventory.model.RemoveCarRequest;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class InventoryCommand implements QuarkusApplication {

    private static final String USAGE =
            "Usage: inventory <add>|<remove> " +
                    "<licensePlatNumber> <manufacturer> <model>";

    @GrpcClient("inventory")
    InventoryService inventoryService;

    @Override
    public int run(String... args) {
        if (args.length < 4) {
            System.out.println(USAGE);
            return 1;
        }

        String command = args[0];
        String licensePlateNumber = args[1];
        String manufacturer = args[2];
        String model = args[3];

        switch (command) {
            case "add":
                add(licensePlateNumber, manufacturer, model);
                break;
            case "remove":
                remove(licensePlateNumber);
                break;
            default:
                System.out.println(USAGE);
                return 1;
        }
        return 0;
    }

    public void add(String licensePlateNumber, String manufacturer, String model) {
        inventoryService.add(InsertCarRequest.newBuilder()
                        .setLicensePlateNumber(licensePlateNumber)
                        .setManufacturer(manufacturer)
                        .setModel(model)
                        .build())
                .onItem().invoke(carResponse -> {
                    System.out.println("Car added: " + carResponse);
                })
                .await().indefinitely();
    }

    public void remove(String licensePlateNumber) {
        inventoryService.remove(RemoveCarRequest.newBuilder()
                        .setLicensePlateNumber(licensePlateNumber)
                        .build())
                .onItem().invoke(carResponse -> {
                    System.out.println("Car removed: " + carResponse);
                })
                .await().indefinitely();
    }
}
