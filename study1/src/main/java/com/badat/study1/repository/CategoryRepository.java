package com.badat.study1.repository;

import com.badat.study1.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    List<Category> findAllByOrderByNameAsc();
    
    List<Category> findByStatusAndIsDeleteFalse(Category.Status status);
}