package com.familyfirstsoftware.contactApi.repo;

import com.familyfirstsoftware.contactApi.domain.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContactRepo extends JpaRepository<Contact, String>{

    Optional<Contact> findById(String id);
    //TODO more like findByEmail ect

}
