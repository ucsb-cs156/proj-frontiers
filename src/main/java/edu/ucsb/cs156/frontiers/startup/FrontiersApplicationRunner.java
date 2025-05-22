package edu.ucsb.cs156.frontiers.startup;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FrontiersApplicationRunner implements ApplicationRunner {

  private final FrontiersStartup frontiersStartup;

  public FrontiersApplicationRunner(FrontiersStartup frontiersStartup) {
    this.frontiersStartup = frontiersStartup;
  }

  @Override
  public void run(ApplicationArguments args) {
    log.info("FrontiersApplicationRunner.run called");
    frontiersStartup.alwaysRunOnStartup();
  }
}
