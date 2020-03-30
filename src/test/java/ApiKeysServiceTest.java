import org.junit.Assert;
import org.junit.Test;
import org.maggus.mikedb.services.ApiKeysService;

public class ApiKeysServiceTest {

    public final String DB_NAME = ":momory:apiKeyTestDB";

    @Test
    public void goodKeyGoodAccessTest() {
        boolean res = ApiKeysService.isValidApiKey("5up3r53cr3tK3y", ApiKeysService.Access.READ, ":memory:testDB");
        Assert.assertTrue(res);
    }

    @Test
    public void goodKeyBadDbNameTest() {
        boolean res = ApiKeysService.isValidApiKey("5up3r53cr3tK3y", ApiKeysService.Access.READ, ":memory:testDB1");
        Assert.assertFalse(res);
    }

    @Test
    public void goodKeyBadAccessTest() {
        boolean res = ApiKeysService.isValidApiKey("T3st53cr3tK3y*", ApiKeysService.Access.WRITE, "memory:.testAnotherDB");
        Assert.assertFalse(res);
    }

    @Test
    public void badKeyTest() {
        boolean res = ApiKeysService.isValidApiKey("5up3r53cr3tK3Y", ApiKeysService.Access.READ, ":memory:testDB");
        Assert.assertFalse(res);
    }

    @Test
    public void goodWildCardKeyTest() {
        boolean res = ApiKeysService.isValidApiKey("T3st53cr3tK3y", ApiKeysService.Access.READ, ":memory:.testAnotherDB");
        Assert.assertTrue(res);
    }

    @Test
    public void badWildCardKeyTest() {
        boolean res = ApiKeysService.isValidApiKey("T3st53cr3tK3y*", ApiKeysService.Access.WRITE, ":memory:testAnotherDB");
        Assert.assertFalse(res);
    }

}
