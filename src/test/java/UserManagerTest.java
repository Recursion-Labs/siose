import me.samarthh.managers.UserManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class UserManagerTest {

    private UserManager userManager;

    @Before
    public void setUp() {
        userManager = new UserManager();
    }

    @After
    public void tearDown() {
        userManager.close();
    }

    @Test
    public void testSetAndGetToken() {
        UUID uuid = UUID.randomUUID();
        String token = "test-token";

        userManager.setToken(uuid, token);

        assertTrue(userManager.isAuthenticated(uuid));
        assertEquals(token, userManager.getToken(uuid));
    }

    @Test
    public void testIsAuthenticatedFalse() {
        UUID uuid = UUID.randomUUID();

        assertFalse(userManager.isAuthenticated(uuid));
        assertNull(userManager.getToken(uuid));
    }
}
