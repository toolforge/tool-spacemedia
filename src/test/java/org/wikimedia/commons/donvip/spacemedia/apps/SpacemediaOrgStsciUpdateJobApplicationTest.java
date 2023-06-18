package org.wikimedia.commons.donvip.spacemedia.apps;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({ "test", "job-webb-nasa" })
@SpringBootTest(classes = SpacemediaOrgStsciUpdateJobApplication.class)
class SpacemediaOrgStsciUpdateJobApplicationTest {

    @Test
    void testContextLoads(ApplicationContext context) {
        assertNotNull(context);
    }
}
