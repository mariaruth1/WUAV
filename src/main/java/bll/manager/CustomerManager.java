package bll.manager;

import be.Customer;
import bll.IManager;
import dal.dao.CustomerDAO;
import dal.factories.DAOFactory;
import utils.enums.BusinessEntityType;
import utils.enums.ResultState;
import utils.enums.UserRole;
import utils.permissions.AccessChecker;
import utils.permissions.RequiresPermission;

import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

public class CustomerManager implements IManager<Customer> {
    private final CustomerDAO dao;
    private AccessChecker checker = new AccessChecker();

    public CustomerManager() {
        dao = (CustomerDAO) DAOFactory.createDAO(BusinessEntityType.CUSTOMER);
    }

    /**
     * Add a customer to the database if logged-in user has sufficient permission
     * @param customer customer to add
     * @return ResultState / NO_PERMISSION
     */
    @Override
    @RequiresPermission({UserRole.ADMINISTRATOR, UserRole.PROJECT_MANAGER, UserRole.TECHNICIAN})
    public ResultState add(Customer customer) {
        if (checker.hasAccess(this.getClass())) {
            return dao.add(customer);
        }
        else {
            return ResultState.NO_PERMISSION;
        }
    }

    /**
     * Update a customer in the database if logged-in user has sufficient permission
     * @param customer customer to update
     * @return ResultState / NO_PERMISSION
     */
    @Override
    @RequiresPermission({UserRole.ADMINISTRATOR, UserRole.PROJECT_MANAGER, UserRole.TECHNICIAN})
    public ResultState update(Customer customer) {
        if (checker.hasAccess(this.getClass())) {
            return dao.update(customer);
        }
        else {
            return ResultState.NO_PERMISSION;
        }
    }

    /**
     * Delete a customer from the database if logged-in user has sufficient permission
     * @param id id of the customer to delete
     * @return ResultState / NO_PERMISSION
     */
    @Override
    @RequiresPermission({UserRole.ADMINISTRATOR, UserRole.PROJECT_MANAGER})
    public ResultState delete(UUID id) {
        if (checker.hasAccess(this.getClass())) {
            return dao.delete(id);
        }
        else {
            return ResultState.NO_PERMISSION;
        }
    }

    /**
     * Get all customers from the database
     * @return Map<UUID, Customer>
     */
    @Override
    public Map<UUID, Customer> getAll() {
        return dao.getAll();
    }

    /**
     * Get a customer by id from the database
     * @param id id of the customer to get
     * @return Customer
     */
    @Override
    public Customer getById(UUID id) {
        return dao.getById(id);
    }

    /**
     * Gets the amount of customers that have a contract that expires in 2 months or less
     */
    @RequiresPermission({UserRole.ADMINISTRATOR, UserRole.PROJECT_MANAGER})
    public int getAlmostExpiredCustomers(HashMap<UUID, Customer> customers){
        if (checker.hasAccess(this.getClass())) {
            return calculateExpiredCustomers(customers);
        }
        return 0;
    }

    /**
     * Calculate the amount of customers that have a contract that expires in 2 months or less
     * Visibility set to public in order to allow for unit testing, should be treated as private
     */
    public int calculateExpiredCustomers(HashMap<UUID, Customer> customers){
        List<Customer> almostExpiredCustomers = new ArrayList<>();
        customers.values().stream().filter(customer ->
                        customer.getLastContract().before((Date.valueOf(LocalDate.now().minusMonths(47)))))
                .forEach(almostExpiredCustomers::add);
        return almostExpiredCustomers.size();
    }
}
