package gui.model;

import be.Customer;
import be.Document;
import bll.CustomerManager;
import bll.IManager;
import bll.ManagerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

public class CustomerModel implements IModel<Customer> {
    private static CustomerModel instance;
    private IManager<Customer> customerManager;
    private HashMap<UUID, Customer> allCustomers;

    private CustomerModel() {
        customerManager = ManagerFactory.createManager(ManagerFactory.ManagerType.CUSTOMER);
        allCustomers = new HashMap<>();
        allCustomers.putAll(customerManager.getAll());
    }

    public static CustomerModel getInstance() {
        if (instance == null) {
            instance = new CustomerModel();
        }
        return instance;
    }

    @Override
    public CompletableFuture<String> add(Customer customer) {
        String message = customerManager.add(customer);

        CompletableFuture<Map<UUID, Customer>> future = CompletableFuture.supplyAsync(() -> customerManager.getAll());
        return future.thenApplyAsync(customers -> {
            allCustomers.clear();
            allCustomers.putAll(customers);
            return message;
        });
    }

    @Override
    public CompletableFuture<String> update(Customer customer) {
        String message = customerManager.update(customer);
        CompletableFuture<Map<UUID, Customer>> future = CompletableFuture.supplyAsync(() -> customerManager.getAll());
        return future.thenApplyAsync(customers -> {
            allCustomers.clear();
            allCustomers.putAll(customers);
            return message;
        });
    }

    @Override
    public CompletableFuture<String> delete(UUID id) {
        String message = customerManager.delete(id);
        CompletableFuture<Map<UUID, Customer>> future = CompletableFuture.supplyAsync(() -> customerManager.getAll());
        return future.thenApplyAsync(customers -> {
            allCustomers.clear();
            allCustomers.putAll(customers);
            return message;
        });
    }

    @Override
    public Map<UUID, Customer> getAll() {
        return allCustomers;
    }

    @Override
    public Customer getById(UUID id) {
        return customerManager.getById(id);
    }
}
