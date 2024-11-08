package com.familyfirstsoftware.contactApi.service;

import com.familyfirstsoftware.contactApi.domain.Contact;
import com.familyfirstsoftware.contactApi.exceptions.ResourceNotFoundException;
import com.familyfirstsoftware.contactApi.repo.ContactRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static constant.Constant.PHOTO_DIRECTORY;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Service
@Slf4j
@Transactional(rollbackOn = Exception.class)
@RequiredArgsConstructor
public class ContactService {

    private final ContactRepo contactRepo;

    public Page<Contact> getAllContacts(int page, int size){
        return contactRepo.findAll(PageRequest.of(page, size, Sort.by("name")));
    }

    public Contact getContact(String id){
        return contactRepo.findById(id).orElseThrow(() -> new RuntimeException("Contact not found"));
    }

    public Contact createContact(Contact contact){
        return contactRepo.save(contact);

    }
    public String deleteContact(String id) {
        if (contactRepo.existsById(id)) {
            contactRepo.deleteById(id);
            return "Contact deleted successfully.";
        } else {
            throw new ResourceNotFoundException("Contact not found with id: " + id);
        }
    }
    public String uploadPhoto(String id, MultipartFile file){
        log.info("Saving picture for user ID: {}", id); // TODO: add error logging
        Contact contact = getContact(id);
        String photoUrl = photoFunction.apply(id, file);
        contact.setPhotoUrl(photoUrl);
        contactRepo.save(contact);
        return photoUrl;
    }

    // gets the file extension
    private final Function<String, String> fileExtension = filename -> Optional.of(filename)
            .filter(name -> name.contains("."))
            .map(name -> "." + name.substring(name.lastIndexOf(".") + 1))
            .orElse(".png");

    // will take a string and a multipart file and returns a string
    private final BiFunction<String, MultipartFile, String> photoFunction = (id, image) -> {
        try {
            // TODO ask where they want to save... not just in downloads
            Path fileStorageLocation = Paths.get(PHOTO_DIRECTORY).toAbsolutePath().normalize();
            if (!Files.exists(fileStorageLocation)) {
                Files.createDirectories(fileStorageLocation);
            }
            String filename = id + fileExtension.apply(image.getOriginalFilename());
            Files.copy(image.getInputStream(), fileStorageLocation.resolve(filename), REPLACE_EXISTING);
            String fileUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/contacts/image/" + filename).toUriString();

            return fileUri;
        } catch (Exception e) {
            throw new RuntimeException("Unable to save image");
        }
    };




}
