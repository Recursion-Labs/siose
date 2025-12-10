import me.samarthh.managers.UserManager;
import me.samarthh.api.SioseApiClient.PropertyRequestResponse;
import com.google.gson.Gson;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class UserManagerTest {

    private UserManager userManager;
    private Gson gson;

    @Before
    public void setUp() {
        userManager = new UserManager();
        gson = new Gson();
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

    @Test
    public void testPropertyRequestResponseParsing() {
        // Test the actual API response format
        String jsonResponse = "{\n" +
                "    \"message\": \"Property request submitted successfully\",\n" +
                "    \"request\": {\n" +
                "        \"id\": \"8a4c1207-8785-45f9-8698-0913a60926ab\",\n" +
                "        \"propertyType\": \"HOUSE\",\n" +
                "        \"location\": \"Test Location\",\n" +
                "        \"price\": 100000,\n" +
                "        \"description\": \"Test property\",\n" +
                "        \"status\": \"PENDING\",\n" +
                "        \"createdAt\": \"2023-12-10T12:00:00Z\",\n" +
                "        \"updatedAt\": \"2023-12-10T12:00:00Z\"\n" +
                "    }\n" +
                "}";

        PropertyRequestResponse response = gson.fromJson(jsonResponse, PropertyRequestResponse.class);

        assertNotNull(response);
        assertTrue(response.isSuccess()); // Should be true because message contains "success"
        assertEquals("Property request submitted successfully", response.getMessage());
        assertNotNull(response.getRequest());
        assertEquals("8a4c1207-8785-45f9-8698-0913a60926ab", response.getPropertyId());
        assertEquals("HOUSE", response.getRequest().getPropertyType());
    }
}
