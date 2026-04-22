package com.app.Genderize.repository;

import com.app.Genderize.model.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ProfileRepository extends JpaRepository<Profile, UUID>, JpaSpecificationExecutor<Profile> {

    Optional<Profile> findByNameIgnoreCase(String name);
}
