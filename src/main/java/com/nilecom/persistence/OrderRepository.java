package com.nilecom.persistence;

import com.nilecom.domain.Order;

import java.util.List;

/** Write + read access to completed orders. Storage-agnostic. */
public interface OrderRepository {
    void save(Order order);
    /** Most recent first. */
    List<Order> findAll();
}
