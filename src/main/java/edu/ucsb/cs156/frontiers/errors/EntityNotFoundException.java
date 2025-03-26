package edu.ucsb.cs156.frontiers.errors;

/**
 * This is an error class for a custom RuntimeException in Java that is used to indicate
 * when an entity of a specific type with a given ID is not found.
 */
public class EntityNotFoundException extends RuntimeException {
  /**
   * Constructor for the exception
   * 
   * @param entityType The class of the entity that was not found, e.g. User.class
   * @param id the id that was being searched for
   */
  public EntityNotFoundException(Class<?> entityType, Object id) {
    super("%s with id %s not found"
      .formatted(entityType.getSimpleName(), id.toString()));
  }
}
