package com.cth.sdm.repository;

import com.cth.sdm.model.Role;
import com.cth.sdm.model.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
