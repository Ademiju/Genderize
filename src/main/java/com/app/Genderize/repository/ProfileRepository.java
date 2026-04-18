package com.app.Genderize.repository;

import com.app.Genderize.model.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {

    Optional<Profile> findByNameIgnoreCase(String name);

    @Query("""
SELECT p FROM Profile p
WHERE (:gender IS NULL OR p.gender ILIKE :gender)
AND (:countryId IS NULL OR p.countryId ILIKE :countryId)
AND (:ageGroup IS NULL OR p.ageGroup ILIKE :ageGroup)
""")
    List<Profile> filter(
            @Param("gender") String gender,
            @Param("countryId") String countryId,
            @Param("ageGroup") String ageGroup
    );


}