package edu.ucsb.cs156.frontiers.controllers;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.errors.NoLinkedOrganizationException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** This class is used to test ApiController and EntityNotFoundException */
@RequestMapping("/dummycontroller")
@RestController
@Validated
public class DummyController extends ApiController {
  void throwConstraintException(@NotNull @Valid Course course) {
    // do nothing
  }

  @GetMapping("")
  public String getById(@RequestParam Long id) throws EntityNotFoundException {
    if (id == 1) {
      return "String1";
    }
    throw new EntityNotFoundException(String.class, id);
  }

  @GetMapping("/noorg")
  public String noOrg(@RequestParam String courseName) throws EntityNotFoundException {
    throw new NoLinkedOrganizationException(courseName);
  }

  @GetMapping("/illegalargument")
  public String illegalArgument(@RequestParam String courseName) {
    throw new IllegalArgumentException(courseName);
  }

  @GetMapping("/interceptorTest")
  public ResponseEntity<String> interceptorTest() {
    return ResponseEntity.ok("OK");
  }

  @GetMapping("/validationexception")
  public void validationException() {
    throw new ConstraintViolationException("Test ConstraintViolationException", Set.of());
  }
}
