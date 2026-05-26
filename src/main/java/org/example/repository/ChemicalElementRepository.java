package org.example.repository;

import org.example.entity.ChemicalElement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChemicalElementRepository extends JpaRepository<ChemicalElement, Long> {
    Optional<ChemicalElement> findBySymbol(String symbol);
}
