package edu.ucsb.cs156.example.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.ucsb.cs156.example.errors.EntityNotFoundException;


/**
 * This class is used to test ApiController and EntityNotFoundException
 */

@RequestMapping("/dummycontroller")
@RestController
public class DummyController extends ApiController {

    @GetMapping("")
    public String getById(@RequestParam Long id) throws EntityNotFoundException {
        if (id == 1) {
            return "String1";
        }
        throw new EntityNotFoundException(String.class, id);
    }
}
