package net.autocrm.api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import net.autocrm.api.model.ClientDetails;

@Repository
public interface ClientDetailsRepo extends JpaRepository<ClientDetails, Integer> {

	Optional<ClientDetails> findById(String id);

}
