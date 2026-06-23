package com.nilecom.persistence;

import com.nilecom.domain.Product;

import java.util.List;
import java.util.Optional;

/** Read access to the product catalogue. Storage-agnostic. */
public interface InventoryRepository {
    Optional<Product> findById(String id);
    List<Product> findAll();
}
