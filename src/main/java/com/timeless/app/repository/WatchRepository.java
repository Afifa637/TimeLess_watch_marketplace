package com.timeless.app.repository;

import com.timeless.app.entity.Watch;
import com.timeless.app.entity.WatchCondition;
import com.timeless.app.entity.WatchStatus;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WatchRepository extends JpaRepository<Watch, Long>, JpaSpecificationExecutor<Watch> {

    Page<Watch> findByStatus(WatchStatus status, Pageable pageable);

    Page<Watch> findByStatusAndBrandInAndPriceBetweenAndCategoryIn(
        WatchStatus status,
        List<String> brands,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        List<String> categories,
        Pageable pageable
    );

    List<Watch> findBySellerId(Long sellerId);

    List<Watch> findBySellerIdAndStatus(Long sellerId, WatchStatus status);

    Page<Watch> findByStatusAndBrandContainingIgnoreCaseOrStatusAndNameContainingIgnoreCase(
        WatchStatus status1,
        String brand,
        WatchStatus status2,
        String name,
        Pageable pageable
    );
    @Query("SELECT MIN(w.price) FROM Watch w WHERE w.status = com.timeless.app.entity.WatchStatus.ACTIVE")
    BigDecimal findMinActivePrice();

    @Query("SELECT MAX(w.price) FROM Watch w WHERE w.status = com.timeless.app.entity.WatchStatus.ACTIVE")
    BigDecimal findMaxActivePrice();

    long countByStatus(WatchStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Watch w WHERE w.id = :id")
    Optional<Watch> findLockedById(@Param("id") Long id);
}
