package bll.manager;

import be.Customer;
import utils.enums.ResultState;
import utils.enums.UserRole;
import bll.IManager;
import dal.dao.CustomerDAO;
import dal.factories.DAOFactory;
import utils.permissions.AccessChecker;
import utils.permissions.RequiresPermission;

import java.util.Map;
import java.util.UUID;

public class CustomerManager implements IManager<Customer> {
    private CustomerDAO dao;
    private AccessChecker checker = new AccessChecker();

    public CustomerManager() {
        dao = (CustomerDAO) DAOFactory.createDAO(DAOFactory.DAOType.CUSTOMER);
    }

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

    @Override
    @RequiresPermission({UserRole.ADMINISTRATOR, UserRole.PROJECT_MANAGER, UserRole.TECHNICIAN})
    public ResultState delete(UUID id) {
        if (checker.hasAccess(this.getClass())) {
            return dao.delete(id);
        }
        else {
            return ResultState.NO_PERMISSION;
        }
    }

    @Override
    public Map<UUID, Customer> getAll() {
        return dao.getAll();
    }

    @Override
    public Customer getById(UUID id) {
        return dao.getById(id);
    }
}
