import lombok.Data;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Assert;
import org.junit.Test;
import org.maggus.mikedb.KeyValuePairsApi;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class KeyValuePairsApiTest extends JerseyTest {

    @Override
    protected Application configure() {
        ResourceConfig config = new ResourceConfig(KeyValuePairsApi.class);
        //config.register(DbService.class);
        //config.packages("org.maggus.mikedb");
        return config;
    }

    private Invocation.Builder decorateRequest(Invocation.Builder target) {
        return target.header("API_KEY", "5up3r53cr3tK3y");
    }

    @Test
    public void badApiKeyTest() throws Exception {
        WebTarget target = target("testDB");
        Exception ex = null;
        try {
            ObjectItem value3 = target.path("testItem3").request(MediaType.APPLICATION_JSON)
                    .header("API_KEY", "WrongHackyKey")
                    .get(new GenericType<ObjectItem>() {
                    });
            System.out.println("item 3: " + value3);
        } catch (javax.ws.rs.InternalServerErrorException e) {
            System.out.println("Expected InternalServerErrorException: " + e.getMessage());
            ex = e;
        }
        Assert.assertNotNull(ex);

        try {
            ObjectItem value3 = target.path("testItem3").request(MediaType.APPLICATION_JSON)
                    // no API_KEY header
                    .get(new GenericType<ObjectItem>() {
                    });
            System.out.println("item 3: " + value3);
        } catch (javax.ws.rs.InternalServerErrorException e) {
            System.out.println("Expected InternalServerErrorException: " + e.getMessage());
            ex = e;
        }
        Assert.assertNotNull(ex);
    }

    @Test
    public void setGetObjectTest() throws Exception {
        ObjectItem item1 = new ObjectItem();
        item1.setName("Test Name");
        item1.setAge(69);
        WebTarget target = target("testDB");
        //put new resource at /testDB/testItem1
        Response response = decorateRequest(target.path("testItem1").request())
                .put(Entity.entity(item1, MediaType.APPLICATION_JSON));
        System.out.println(response.getHeaderString("Location"));
        System.out.println(response.getStatus());
        Assert.assertEquals(201, response.getStatus());

        //put one more item at /testDB/testItem2
        ObjectItem item2 = new ObjectItem();
        item2.setName("Test Name 2");
        item2.setAge(420);
        response = decorateRequest(target.path("testItem2").request())
                .put(Entity.entity(item2, MediaType.APPLICATION_JSON));
        System.out.println(response.getHeaderString("Location"));
        System.out.println(response.getStatus());
        Assert.assertEquals(201, response.getStatus());

        // get an existing item
        ObjectItem value1 = decorateRequest(target.path("testItem1").request(MediaType.APPLICATION_JSON))
                .get(new GenericType<ObjectItem>() {
                });
        System.out.println("item 1: " + value1);
        Assert.assertNotNull(value1);

        // get an existing item
        ObjectItem value2 = decorateRequest(target.path("testItem2").request(MediaType.APPLICATION_JSON))
                .get(new GenericType<ObjectItem>() {
                });
        System.out.println("item 2: " + value2);
        Assert.assertNotNull(value1);

        // try to get un-existing item
        ObjectItem value3 = decorateRequest(target.path("testItem3").request(MediaType.APPLICATION_JSON))
                .get(new GenericType<ObjectItem>() {
                });
        System.out.println("item 3: " + value3);
        Assert.assertNull(value3);

        // delete item
        response = decorateRequest(target.path("testItem2").request())
                .delete();
        System.out.println(response.getStatus());
        Assert.assertEquals(200, response.getStatus());

        // delete same item again
        response = decorateRequest(target.path("testItem2").request())
                .delete();
        System.out.println(response.getStatus());
        Assert.assertEquals(204, response.getStatus());

        // try to get a deleted item
        value2 = decorateRequest(target.path("testItem2").request(MediaType.APPLICATION_JSON))
                .get(new GenericType<ObjectItem>() {
                });
        System.out.println("item 2: " + value2);
        Assert.assertNull(value2);
    }

    @Test
    public void setGetPrimitiveTest() throws Exception {
        WebTarget target = target("testDB");

        //put new String at /testDB/testString1
        String item1 = "Test String 5";
        Response response = decorateRequest(target.path("testString5").request())
                .put(Entity.entity(item1, MediaType.TEXT_PLAIN));
        System.out.println(response.getHeaderString("Location"));
        System.out.println(response.getStatus());
        Assert.assertEquals(201, response.getStatus());

        // get an existing item
        String value1 = decorateRequest(target.path("testString5").request())
                .get(new GenericType<String>() {
                });
        System.out.println("item 1: " + value1);
        Assert.assertEquals(item1, value1);

        //put new number at /testDB/testString1
        Integer item2 = 69420;
        response = decorateRequest(target.path("testInteger1").request())
                .put(Entity.entity(item2, MediaType.APPLICATION_JSON));
        System.out.println(response.getHeaderString("Location"));
        System.out.println(response.getStatus());
        Assert.assertEquals(201, response.getStatus());

        // get an existing item
        Integer value2 = decorateRequest(target.path("testInteger1").request())
                .get(new GenericType<Integer>() {
                });
        System.out.println("item 2: " + value2);
        Assert.assertEquals(item2, value2);

        //put new number at /testDB/testString1
        Date item3 = new Date();
        response = decorateRequest(target.path("testDate1").request())
                .put(Entity.entity(item3, MediaType.APPLICATION_JSON));
        System.out.println(response.getHeaderString("Location"));
        System.out.println(response.getStatus());
        Assert.assertEquals(201, response.getStatus());

        // get an existing item
        Date value3 = decorateRequest(target.path("testDate1").request())
                .get(new GenericType<Date>() {
                });
        System.out.println("item 3: " + value3);
        Assert.assertEquals(item3, value3);
    }

    @Test
    public void putItemsTest() throws Exception {
        WebTarget target = target("testDB");

        //post new resource at /testDB/testString1
        String item1 = "Test String 1";
        Response response = decorateRequest(target.path("testString1").request())
                .put(Entity.entity(new String[]{item1}, MediaType.APPLICATION_JSON));
        System.out.println(response.getHeaderString("Location"));
        System.out.println(response.getStatus());
        Assert.assertEquals(201, response.getStatus());

        // get items count
        int value = decorateRequest(target.path("testString1").request())
                .head().getLength();
        System.out.println("item num: " + value);
        Assert.assertEquals(1, value);

        //replace with another item to existing resource at /testDB/testString1
        String item2 = "Test String 2";
        response = decorateRequest(target.path("testString1").request())
                .put(Entity.entity(new String[]{item2}, MediaType.APPLICATION_JSON));
        System.out.println(response.getHeaderString("Location"));
        System.out.println(response.getStatus());
        Assert.assertEquals(201, response.getStatus());

        // get new items count
        value = decorateRequest(target.path("testString1").request())
                .head().getLength();
        System.out.println("item num: " + value);
        Assert.assertEquals(1, value);

        // get all items
        List values = decorateRequest(target.path("testString1").request(MediaType.APPLICATION_JSON))
                .get(new GenericType<List>() {
                });
        System.out.println("items: " + values);
        Assert.assertNotNull(values);
        Assert.assertEquals(1, values.size());
    }

    @Test
    public void postItemsTest() throws Exception {
        WebTarget target = target("testDB");

        //post new resource at /testDB/testString1
        String item1 = "Test String 1";
        Response response = decorateRequest(target.path("testString1").request())
                .put(Entity.entity(new String[]{item1}, MediaType.APPLICATION_JSON));
        System.out.println(response.getHeaderString("Location"));
        System.out.println(response.getStatus());
        Assert.assertEquals(201, response.getStatus());

        // get items count
        int value = decorateRequest(target.path("testString1").request())
                .head().getLength();
        System.out.println("item num: " + value);
        Assert.assertEquals(1, value);

        //add another item to existing resource at /testDB/testString1
        String item2 = "Test String 2";
        response = decorateRequest(target.path("testString1").request())
                .post(Entity.entity(new String[]{item2}, MediaType.APPLICATION_JSON));
        System.out.println(response.getHeaderString("Location"));
        System.out.println(response.getStatus());
        Assert.assertEquals(201, response.getStatus());

        // get new items count
        value = decorateRequest(target.path("testString1").request())
                .head().getLength();
        System.out.println("item num: " + value);
        Assert.assertEquals(2, value);

        // get all items
        List values = decorateRequest(target.path("testString1").request())
                .get(new GenericType<List>() {
                });
        System.out.println("items: " + values);
        Assert.assertNotNull(values);
        Assert.assertEquals(2, values.size());
    }

    @Test
    public void quieryItemsTest() throws Exception {
        WebTarget target = target("testDB");

        //post new resource at /testDB/testString1
        Response response = decorateRequest(target.path("testString1").request())
                .put(Entity.entity(new String[]{"Test String 1"}, MediaType.APPLICATION_JSON));
//        System.out.println(response.getHeaderString("Location"));
//        System.out.println(response.getStatus());
        Assert.assertEquals(201, response.getStatus());

        response = decorateRequest(target.path("testString1").request())
                .post(Entity.entity(new String[]{"Test String 2"}, MediaType.APPLICATION_JSON));
//        System.out.println(response.getHeaderString("Location"));
//        System.out.println(response.getStatus());
        Assert.assertEquals(201, response.getStatus());

        response = decorateRequest(target.path("testString1").request())
                .post(Entity.entity(new String[]{"Test String 3"}, MediaType.APPLICATION_JSON));
//        System.out.println(response.getHeaderString("Location"));
//        System.out.println(response.getStatus());
        Assert.assertEquals(201, response.getStatus());

        // get items count
        int value = decorateRequest(target.path("testString1").request())
                .head().getLength();
        System.out.println("item num: " + value);
        Assert.assertEquals(3, value);

        // get subset of items
        int firstResult = 0;
        int maxResults = 1;
        List values = decorateRequest(target.path("testString1").
                queryParam("firstResult", firstResult).queryParam("maxResults", maxResults)
                .request())
                .get(new GenericType<List>() {
                });
        System.out.println("items: " + values);
        Assert.assertNotNull(values);
        Assert.assertEquals(1, values.size());
        Assert.assertEquals("Test String 1", values.get(0));

        // get subset of items
        firstResult = 1;
        maxResults = 2;
        values = decorateRequest(target.path("testString1")
                .queryParam("firstResult", firstResult).queryParam("maxResults", maxResults)
                .request(MediaType.APPLICATION_JSON))
                .get(new GenericType<List>() {
                });
        System.out.println("items: " + values);
        Assert.assertNotNull(values);
        Assert.assertEquals(2, values.size());
        Assert.assertEquals("Test String 2", values.get(0));
    }

    @Test
    public void largeValueTest() throws Exception {
        WebTarget target = target("testDB");

        //post new resource at /testDB/testString1
        Response response = decorateRequest(target.path("testItemsList").request())
                .put(Entity.entity(makeObjectItemList(1024), MediaType.APPLICATION_JSON));
//        System.out.println(response.getHeaderString("Location"));
//        System.out.println(response.getStatus());
        Assert.assertEquals(201, response.getStatus());

        // get items count
        int value = decorateRequest(target.path("testItemsList").request())
                .head().getLength();
        System.out.println("item num: " + value);
        Assert.assertEquals(1024, value);

        // get subset of items
        int firstResult = 0;
        int maxResults = -1;
        List values = decorateRequest(target.path("testItemsList").
                queryParam("firstResult", firstResult).queryParam("maxResults", maxResults)
                .request(MediaType.APPLICATION_JSON))
                .get(new GenericType<List>() {
                });
        //System.out.println("items: " + values);
        Assert.assertNotNull(values);
        Assert.assertEquals(1024, values.size());

        // delete large value
        response = decorateRequest(target.path("testItemsList").request())
                .delete();
//        System.out.println(response.getHeaderString("Location"));
//        System.out.println(response.getStatus());
        Assert.assertEquals(200, response.getStatus());

        value = decorateRequest(target.path("testItemsList").request())
                .head().getLength();
        System.out.println("item num: " + value);
        Assert.assertEquals(0, value);
    }

    private List<ObjectItem> makeObjectItemList(int num){
        List<ObjectItem> items  = new ArrayList<>(num);
        for (int i = 0; i < num; i++) {
            ObjectItem item = new ObjectItem();
            item.setName("Object Item #" + i + 1);
            item.setAge(i);
            items.add(item);
        }
        return items;
    }
}

@Data
class ObjectItem {
    private String name;
    private Integer age;
}