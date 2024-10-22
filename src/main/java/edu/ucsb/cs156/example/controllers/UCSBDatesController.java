package edu.ucsb.cs156.example.controllers;

import edu.ucsb.cs156.example.entities.UCSBDate;
import edu.ucsb.cs156.example.errors.EntityNotFoundException;
import edu.ucsb.cs156.example.repositories.UCSBDateRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import java.time.LocalDateTime;

/**
 * This is a REST controller for UCSBDates
 */

@Tag(name = "UCSBDates")
@RequestMapping("/api/ucsbdates")
@RestController
@Slf4j
public class UCSBDatesController extends ApiController {

    @Autowired
    UCSBDateRepository ucsbDateRepository;

    /**
     * List all UCSB dates
     * 
     * @return an iterable of UCSBDate
     */
    @Operation(summary= "List all ucsb dates")
    @PreAuthorize("hasRole('ROLE_USER')")
    @GetMapping("/all")
    public Iterable<UCSBDate> allUCSBDates() {
        Iterable<UCSBDate> dates = ucsbDateRepository.findAll();
        return dates;
    }

    /**
     * Get a single date by id
     * 
     * @param id the id of the date
     * @return a UCSBDate
     */
    @Operation(summary= "Get a single date")
    @PreAuthorize("hasRole('ROLE_USER')")
    @GetMapping("")
    public UCSBDate getById(
            @Parameter(name="id") @RequestParam Long id) {
        UCSBDate ucsbDate = ucsbDateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(UCSBDate.class, id));

        return ucsbDate;
    }

    /**
     * Create a new date
     * 
     * @param quarterYYYYQ  the quarter in the format YYYYQ
     * @param name          the name of the date
     * @param localDateTime the date
     * @return the saved ucsbdate
     */
    @Operation(summary= "Create a new date")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/post")
    public UCSBDate postUCSBDate(
            @Parameter(name="quarterYYYYQ") @RequestParam String quarterYYYYQ,
            @Parameter(name="name") @RequestParam String name,
            @Parameter(name="localDateTime", description="date (in iso format, e.g. YYYY-mm-ddTHH:MM:SS; see https://en.wikipedia.org/wiki/ISO_8601)") @RequestParam("localDateTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime localDateTime)
            throws JsonProcessingException {

        // For an explanation of @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        // See: https://www.baeldung.com/spring-date-parameters

        log.info("localDateTime={}", localDateTime);

        UCSBDate ucsbDate = new UCSBDate();
        ucsbDate.setQuarterYYYYQ(quarterYYYYQ);
        ucsbDate.setName(name);
        ucsbDate.setLocalDateTime(localDateTime);

        UCSBDate savedUcsbDate = ucsbDateRepository.save(ucsbDate);

        return savedUcsbDate;
    }

    /**
     * Delete a UCSBDate
     * 
     * @param id the id of the date to delete
     * @return a message indicating the date was deleted
     */
    @Operation(summary= "Delete a UCSBDate")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("")
    public Object deleteUCSBDate(
            @Parameter(name="id") @RequestParam Long id) {
        UCSBDate ucsbDate = ucsbDateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(UCSBDate.class, id));

        ucsbDateRepository.delete(ucsbDate);
        return genericMessage("UCSBDate with id %s deleted".formatted(id));
    }

    /**
     * Update a single date
     * 
     * @param id       id of the date to update
     * @param incoming the new date
     * @return the updated date object
     */
    @Operation(summary= "Update a single date")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("")
    public UCSBDate updateUCSBDate(
            @Parameter(name="id") @RequestParam Long id,
            @RequestBody @Valid UCSBDate incoming) {

        UCSBDate ucsbDate = ucsbDateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(UCSBDate.class, id));

        ucsbDate.setQuarterYYYYQ(incoming.getQuarterYYYYQ());
        ucsbDate.setName(incoming.getName());
        ucsbDate.setLocalDateTime(incoming.getLocalDateTime());

        ucsbDateRepository.save(ucsbDate);

        return ucsbDate;
    }
}
