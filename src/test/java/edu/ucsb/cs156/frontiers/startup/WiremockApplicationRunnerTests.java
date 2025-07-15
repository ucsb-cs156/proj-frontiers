package edu.ucsb.cs156.frontiers.startup;

import edu.ucsb.cs156.frontiers.services.wiremock.WiremockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.ApplicationArguments;

import static org.mockito.Mockito.*;

class WiremockApplicationRunnerTests {



    @Mock
    private WiremockService wiremockService;

    @Mock
    private ApplicationArguments mockArgs;

    @InjectMocks
    private WiremockApplicationRunner wiremockApplicationRunner;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void test_run_calls_AlwaysRunOnStartup() throws Exception {
        wiremockApplicationRunner.run(mockArgs);
        verify(wiremockService, times(1)).init();
    }
}
