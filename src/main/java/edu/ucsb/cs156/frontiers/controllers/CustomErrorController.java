package edu.ucsb.cs156.frontiers.controllers;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Custom error controller that replaces the default white label error page with a more
 * user-friendly error page.
 */
@Controller
public class CustomErrorController implements ErrorController {

  /**
   * Handles error requests and returns a custom error page.
   *
   * @param request the HTTP request
   * @param model the model to pass attributes to the view
   * @return the name of the error view template
   */
  @RequestMapping("/error")
  public String handleError(HttpServletRequest request, Model model) {
    // Get error status
    Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
    int statusCode = 500; // Default to internal server error

    if (status != null) {
      statusCode = Integer.parseInt(status.toString());
    }

    // Get error message
    String errorMessage = "An unexpected error occurred";
    if (statusCode == 404) {
      errorMessage =
          "The page you are looking for might have been removed or is temporarily unavailable";
    } else if (statusCode == 403) {
      errorMessage = "You don't have permission to access this resource";
    } else if (statusCode == 500) {
      errorMessage = "We're sorry, something went wrong on our end";
    }

    // Get exception details for debugging
    Throwable throwable = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
    String exceptionMessage =
        throwable != null ? throwable.getMessage() : "No exception details available";

    // Get stack trace
    String stackTrace = "";
    if (throwable != null) {
      for (StackTraceElement element : throwable.getStackTrace()) {
        stackTrace += element.toString() + "\n";
      }
    }

    // Add attributes to the model
    model.addAttribute("status", statusCode);
    model.addAttribute("error", HttpStatus.valueOf(statusCode).getReasonPhrase());
    model.addAttribute("message", errorMessage);
    model.addAttribute("exceptionMessage", exceptionMessage);
    model.addAttribute("stackTrace", stackTrace);
    model.addAttribute("timestamp", java.time.LocalDateTime.now());
    model.addAttribute("path", request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI));

    return "error";
  }
}
