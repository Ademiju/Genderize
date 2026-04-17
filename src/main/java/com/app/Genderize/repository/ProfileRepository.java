package com.app.Genderize.repository;

import com.app.Genderize.model.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {

    Optional<Profile> findByNameIgnoreCase(String name);

    @Query("""
        SELECT p FROM Profile p
        WHERE (:gender IS NULL OR LOWER(p.gender) = LOWER(:gender))
        AND (:countryId IS NULL OR LOWER(p.countryId) = LOWER(:countryId))
        AND (:ageGroup IS NULL OR LOWER(p.ageGroup) = LOWER(:ageGroup))
    """)
    List<Profile> filter(String gender, String countryId, String ageGroup);
}