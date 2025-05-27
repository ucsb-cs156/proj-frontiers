package edu.ucsb.cs156.frontiers.startup;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

class FrontiersApplicationRunnerTests {

    @Test
    void testRunCallsStartup() throws Exception {
        // mock dependencies
        FrontiersStartup mockStartup = mock(FrontiersStartup.class);
        ApplicationArguments mockArgs = mock(ApplicationArguments.class);

        // instance under test
        FrontiersApplicationRunner runner = new FrontiersApplicationRunner();
        ReflectionTestUtils.setField(runner, "frontiersStartup", mockStartup);

        // act
        runner.run(mockArgs);

        // assert
        verify(mockStartup, times(1)).alwaysRunOnStartup();
    }
}
