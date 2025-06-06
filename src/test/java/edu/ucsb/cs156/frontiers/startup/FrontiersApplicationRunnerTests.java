package edu.ucsb.cs156.frontiers.startup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.boot.ApplicationArguments;

import static org.mockito.Mockito.*;

class FrontiersApplicationRunnerTests {

    @Mock
    private FrontiersStartup mockStartup;

    private FrontiersApplicationRunner runner;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        runner = new FrontiersApplicationRunner(mockStartup);
    }

    @Test
    void run_shouldInvokeAlwaysRunOnStartup() throws Exception {
        // arrange
        ApplicationArguments args = mock(ApplicationArguments.class);

        // act
        runner.run(args);

        // assert
        verify(mockStartup, times(1)).alwaysRunOnStartup();
    }
}
