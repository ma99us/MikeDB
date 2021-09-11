import lombok.Data;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Assert;
import org.junit.Test;
import org.maggus.mikedb.DbHttpApiResource;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class DbHttpApiResourceTest extends JerseyTest {

    @Override
    protected Application configure() {
        ResourceConfig config = new ResourceConfig(DbHttpApiResource.class);
        //config.register(DbService.class);
        //config.packages("org.maggus.mikedb");
        return config;
    }

    private Invocation.Builder decorateRequest(Invocation.Builder target) {
        return target.header("API_KEY", "5up3r53cr3tK3y");
    }

    @Test
    public void dropDb() {
        // open database and put something in it
        WebTarget target = target("testDB");
        Response response = decorateRequest(target.path("deleteMe").request())
                .put(Entity.entity(new ObjectItem(), MediaType.APPLICATION_JSON));
        Assert.assertEquals(201, response.getStatus());

        // drop the whole db
        response = decorateRequest(target.path("/").request())
                .delete();
        Assert.assertEquals(200, response.getStatus());

        // try to drop it again
        response = decorateRequest(target.path("/").request())
                .delete();
        Assert.assertEquals(204, response.getStatus());
    }

    @Test
    public void badApiKeyTest() throws Exception {
        WebTarget target = target("testDB");
        Exception ex = null;
        try {
            ObjectItem value3 = target.path("testItem3").request()
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
            ObjectItem value3 = target.path("testItem3").request()
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
        Assert.assertTrue(value1.getId() != null && value1.getId() > 0);

        // get an existing item
        ObjectItem value2 = decorateRequest(target.path("testItem2").request(MediaType.APPLICATION_JSON))
                .get(new GenericType<ObjectItem>() {
                });
        System.out.println("item 2: " + value2);
        Assert.assertNotNull(value2);
        Assert.assertTrue(value2.getId() != null && value2.getId() > 0);

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
        Assert.assertEquals(200, response.getStatus());

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
        Response response = decorateRequest(target.path("testString11").request())
                .post(Entity.entity(item1, MediaType.TEXT_PLAIN));
        System.out.println(response.getHeaderString("Location"));
        System.out.println(response.getStatus());
        Assert.assertEquals(201, response.getStatus());

        // get items count
        int num = decorateRequest(target.path("testString11").request())
                .head().getLength();
        System.out.println("item num: " + num);
        Assert.assertEquals(1, num);

        // get item
        String value = decorateRequest(target.path("testString11").request())
                .get(new GenericType<String>() {
                });
        System.out.println("item: " + value);
        Assert.assertNotNull(value);

        //add another item to existing resource at /testDB/testString1
        String item2 = "Test String 2";
        response = decorateRequest(target.path("testString11").request())
                .post(Entity.entity(item2, MediaType.TEXT_PLAIN));
        System.out.println(response.getHeaderString("Location"));
        System.out.println(response.getStatus());
        Assert.assertEquals(200, response.getStatus());

        // get new items count
        num = decorateRequest(target.path("testString11").request())
                .head().getLength();
        System.out.println("item num: " + num);
        Assert.assertEquals(2, num);

        // get item
        List values = decorateRequest(target.path("testString11").request())
                .get(new GenericType<List>() {
                });
        System.out.println("items: " + values);
        Assert.assertNotNull(values);
        Assert.assertEquals(2, values.size());
    }

    @Test
    public void reorderItemsTest() throws Exception {
        WebTarget target = target("testDB");

        // clean up
        decorateRequest(target.path("testObject8").request()).delete();

        //add a bunch of objects with ids
        ObjectItem item1 = new ObjectItem();
        item1.setName("Test Object 1");
        item1.setAge(123);
        item1.setId(12345L);
        ObjectItem item2 = new ObjectItem();
        item2.setName("Test Object 2");
        item2.setAge(456);
        item2.setId(23456L);
        target.property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true);
        Response response = decorateRequest(target.path("testObject8").request())
                .method("PATCH", Entity.entity(new ObjectItem[]{item1, item2}, MediaType.APPLICATION_JSON));
        System.out.println(response.getStatus());
        Assert.assertEquals(201, response.getStatus());

        // get all items
        List<ObjectItem> values = decorateRequest(target.path("testObject8").request())
                .get(new GenericType<List<ObjectItem>>() {
                });
        System.out.println("items: " + values);
        Assert.assertNotNull(values);
        Assert.assertEquals(2, values.size());
        Assert.assertEquals("Test Object 2", values.get(1).getName());

        // re-order items
        target.property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true);
        response = decorateRequest(target.path("testObject8").queryParam("index", 0).request())
                .method("PATCH", Entity.entity(item2, MediaType.APPLICATION_JSON));
        System.out.println(response.getStatus());
        Assert.assertEquals(200, response.getStatus());

        // get new items count
        values = decorateRequest(target.path("testObject8").request())
                .get(new GenericType<List<ObjectItem>>() {
                });
        System.out.println("items: " + values);
        Assert.assertEquals(2, values.size());
        Assert.assertEquals("Test Object 2", values.get(0).getName());
    }


    @Test
    public void postItemsListTest() throws Exception {
        WebTarget target = target("testDB");

        //post new resource at /testDB/testString1
        ObjectItem item1 = new ObjectItem();
        item1.setName("Test Object 1");
        item1.setAge(123);
        Response response = decorateRequest(target.path("testObject6").request())
                .put(Entity.entity(new ArrayList<ObjectItem>() {{
                    add(item1);
                }}, MediaType.APPLICATION_JSON));
        System.out.println(response.getHeaderString("Location"));
        System.out.println(response.getStatus());
        Assert.assertEquals(201, response.getStatus());

        // get items count
        int value = decorateRequest(target.path("testObject6").request())
                .head().getLength();
        System.out.println("item num: " + value);
        Assert.assertEquals(1, value);

        //add another item to existing resource at /testDB/testString1
        ObjectItem item2 = new ObjectItem();
        item2.setName("Test Object 2");
        item2.setAge(456);
        response = decorateRequest(target.path("testObject6").request())
                .post(Entity.entity(item2, MediaType.APPLICATION_JSON));
        System.out.println(response.getHeaderString("Location"));
        System.out.println(response.getStatus());
        Assert.assertEquals(200, response.getStatus());

        // get new items count
        value = decorateRequest(target.path("testObject6").request())
                .head().getLength();
        System.out.println("item num: " + value);
        Assert.assertEquals(2, value);

        // get all items
        List values = decorateRequest(target.path("testObject6").request())
                .get(new GenericType<List>() {
                });
        System.out.println("items: " + values);
        Assert.assertNotNull(values);
        Assert.assertEquals(2, values.size());

        // add a bunch of items
        ObjectItem item3 = new ObjectItem();
        item3.setName("Test Object 3");
        item3.setAge(789);
        ObjectItem item4 = new ObjectItem();
        item4.setName("Test Object 4");
        item4.setAge(321);
        response = decorateRequest(target.path("testObject6").request())
                .post(Entity.entity(new ObjectItem[]{item3, item4}, MediaType.APPLICATION_JSON));
        System.out.println(response.getHeaderString("Location"));
        System.out.println(response.getStatus());
        Assert.assertEquals(200, response.getStatus());

        // get new items count
        value = decorateRequest(target.path("testObject6").request())
                .head().getLength();
        System.out.println("item num: " + value);
        Assert.assertEquals(4, value);

        // insert a bunch of primitive items
        response = decorateRequest(target.path("testObject6")
                .queryParam("index", 1).request())
                .post(Entity.entity(new String[]{"Test String 3", "Test String 4"}, MediaType.APPLICATION_JSON));
        System.out.println(response.getHeaderString("Location"));
        System.out.println(response.getStatus());
        Assert.assertEquals(200, response.getStatus());

        // get new items count
        value = decorateRequest(target.path("testObject6").request())
                .head().getLength();
        System.out.println("item num: " + value);
        Assert.assertEquals(6, value);

        // get all items
        values = decorateRequest(target.path("testObject6").request())
                .get(new GenericType<List>() {
                });
        Assert.assertNotNull(values);
        Assert.assertTrue(values.get(1) instanceof String);
        Assert.assertEquals("Test String 3", values.get(1));
        Assert.assertTrue(values.get(3) instanceof Map);
        Assert.assertEquals("Test Object 2", ((Map)values.get(3)).get("name"));
        Assert.assertTrue((Long)((Map)values.get(3)).get("id") > 0);
    }


    @Test
    public void putDeleteItemsTest() throws Exception {
        WebTarget target = target("testDB");

        // clean up
        decorateRequest(target.path("testString4").request()).delete();

        // add single item
        Response response = decorateRequest(target.path("testString4").request())
                .put(Entity.entity("Test String 4", MediaType.TEXT_PLAIN));
        Assert.assertEquals(201, response.getStatus());

        // get items count
        int num = decorateRequest(target.path("testString4").request())
                .head().getLength();
        System.out.println("item num: " + num);
        Assert.assertEquals(1, num);

        // delete item
        response = decorateRequest(target.path("testString4").request())
                .delete();
        Assert.assertEquals(200, response.getStatus());

        // get items count
        num = decorateRequest(target.path("testString4").request())
                .head().getLength();
        System.out.println("item num: " + num);
        Assert.assertEquals(0, num);

        // try to delete again
        response = decorateRequest(target.path("testString4").request())
                .delete();
        Assert.assertEquals(204, response.getStatus());
    }

    @Test
    public void deleteObjectsByKeyTest() throws Exception {
        WebTarget target = target("testDB");
        // add a few objects
        ObjectItem item1 = new ObjectItem();
        item1.setName("Test Object 1");
        item1.setAge(123);
        ObjectItem item2 = new ObjectItem();
        item2.setName("Test Object 2");
        item2.setAge(456);
        Response response = decorateRequest(target.path("testObject4").request())
                .put(Entity.entity(new ObjectItem[]{item1, item2}, MediaType.APPLICATION_JSON));
        Assert.assertEquals(201, response.getStatus());

        // get items count
        int num = decorateRequest(target.path("testObject4").request())
                .head().getLength();
        System.out.println("item num: " + num);
        Assert.assertEquals(2, num);

        List<ObjectItem> values = decorateRequest(target.path("testObject4").request())
                .get(new GenericType<List<ObjectItem>>() {
                });
        Assert.assertEquals(2, values.size());
        ObjectItem item3 = values.get(0);

        // delete item by id
        response = decorateRequest(target.path("testObject4")
                .queryParam("id", item3.getId()).request())
                .delete();
        Assert.assertEquals(200, response.getStatus());

        // get items count
        num = decorateRequest(target.path("testObject4").request())
                .head().getLength();
        System.out.println("item num: " + num);
        Assert.assertEquals(1, num);

        // delete by object
//        target.property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true);
//        response = decorateRequest(target.path("testObject4").request())
//                .method("DELETE", Entity.entity(values.get(1), MediaType.APPLICATION_JSON));
//        Assert.assertEquals(200, response.getStatus());
//
//        // get items count
//        num = decorateRequest(target.path("testObject4").request())
//                .head().getLength();
//        System.out.println("item num: " + num);
//        Assert.assertEquals(0, num);

        // try to delete again
        response = decorateRequest(target.path("testObject4")
                .queryParam("id", item3.getId()).request())
                .delete();
        Assert.assertEquals(204, response.getStatus());

        values = decorateRequest(target.path("testObject4").request())
                .get(new GenericType<List<ObjectItem>>() {
                });
        Assert.assertEquals(1, values.size());
        Assert.assertEquals("Test Object 2", values.get(0).getName());
    }

    @Test
    public void deleteObjectByIndexTest() throws Exception {
        WebTarget target = target("testDB");

        // clean up
        decorateRequest(target.path("testString44").request()).delete();

        // add a few objects
        ObjectItem item1 = new ObjectItem();
        item1.setName("Test Object 1");
        item1.setAge(123);
        ObjectItem item2 = new ObjectItem();
        item2.setName("Test Object 2");
        item2.setAge(456);
        ObjectItem item3 = new ObjectItem();
        item3.setName("Test Object 3");
        item3.setAge(789);
        Response response = decorateRequest(target.path("testString44").request())
                .put(Entity.entity(new ObjectItem[]{item1, item2, item3}, MediaType.APPLICATION_JSON));
        Assert.assertEquals(201, response.getStatus());

        // get items count
        int num = decorateRequest(target.path("testString44").request())
                .head().getLength();
        System.out.println("item num: " + num);
        Assert.assertEquals(3, num);

        List<ObjectItem> values = decorateRequest(target.path("testString44").request())
                .get(new GenericType<List<ObjectItem>>() {
                });
        Assert.assertEquals(new Integer(123), values.get(0).getAge());
        Assert.assertEquals(new Integer(456), values.get(1).getAge());
        Assert.assertEquals(new Integer(789), values.get(2).getAge());

        // delete item by index
        response = decorateRequest(target.path("testString44")
                .queryParam("index", 1).request())
                .delete();
        Assert.assertEquals(200, response.getStatus());

        // get items count
        num = decorateRequest(target.path("testString44").request())
                .head().getLength();
        System.out.println("item num: " + num);
        Assert.assertEquals(2, num);

        values = decorateRequest(target.path("testString44").request())
                .get(new GenericType<List<ObjectItem>>() {
                });
        Assert.assertEquals(new Integer(123), values.get(0).getAge());
        Assert.assertEquals(new Integer(789), values.get(1).getAge());
    }

    @Test
    public void postMultipleItemsTest() throws Exception {
        WebTarget target = target("testDB");

        // make sure it is a new record
        Response response = decorateRequest(target.path("testString4").request()).delete();

        // add single item
        response = decorateRequest(target.path("testString4").request())
                .post(Entity.entity("Test String 4", MediaType.TEXT_PLAIN));
        Assert.assertEquals(201, response.getStatus());

        // get items count
        int num = decorateRequest(target.path("testString4").request())
                .head().getLength();
        System.out.println("item num: " + num);
        Assert.assertEquals(1, num);

         // get item
        List values = decorateRequest(target.path("testString4").request())
                .get(new GenericType<List>() {
                });
        System.out.println("items: " + values);
        Assert.assertNotNull(values);
        Assert.assertEquals(1, values.size());

        // add multiple items
        response = decorateRequest(target.path("testString5").request()).delete();
        response = decorateRequest(target.path("testString5").request())
                .post(Entity.entity(new String[]{"Test String 5", "Test String 6", "Test String 7"},
                        MediaType.APPLICATION_JSON));
        Assert.assertEquals(201, response.getStatus());

        // get items count
        num = decorateRequest(target.path("testString5").request())
                .head().getLength();
        System.out.println("item num: " + num);
        Assert.assertEquals(3, num);

        // get item
        values = decorateRequest(target.path("testString5").request())
                .get(new GenericType<List>() {
                });
        System.out.println("items: " + values);
        Assert.assertNotNull(values);
        Assert.assertEquals(3, values.size());
    }

    @Test
    public void queryItemsTest() throws Exception {
        WebTarget target = target("testDB");

        // clean up
        decorateRequest(target.path("testString1").request()).delete();

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
        Assert.assertEquals(200, response.getStatus());

        response = decorateRequest(target.path("testString1").request())
                .post(Entity.entity(new String[]{"Test String 3"}, MediaType.APPLICATION_JSON));
//        System.out.println(response.getHeaderString("Location"));
//        System.out.println(response.getStatus());
        Assert.assertEquals(200, response.getStatus());

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

        // clean up
        decorateRequest(target.path("testItemsList").request()).delete();

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

    @Test
    public void inMemoryDbTest() throws Exception {
        ObjectItem item1 = new ObjectItem();
        item1.setName("Test Name");
        item1.setAge(69);
        WebTarget target = target(":memory:testDB");

        //put new resource at /testDB/testItem1
        Response response = decorateRequest(target.path("testItem1").request())
                .put(Entity.entity(item1, MediaType.APPLICATION_JSON));
        System.out.println(response.getHeaderString("Location"));
        System.out.println(response.getStatus());
        Assert.assertEquals(201, response.getStatus());

        // get the item back
        ObjectItem value = decorateRequest(target.path("testItem1").request(MediaType.APPLICATION_JSON))
                .get(new GenericType<ObjectItem>() {
                });
        System.out.println("item: " + value);
        Assert.assertNotNull(value);
    }

    @Test
    public void getItemWithFilteredFieldsTest() throws Exception {
        ObjectItem item1 = new ObjectItem();
        item1.setName("Test Name");
        item1.setAge(72);
        WebTarget target = target("testDB");

        // clean up
        decorateRequest(target.path("testItemToFilter").request()).delete();

        //put new object item
        Response response = decorateRequest(target.path("testItemToFilter").request())
                .put(Entity.entity(item1, MediaType.APPLICATION_JSON));
        System.out.println(response.getHeaderString("Location"));
        System.out.println(response.getStatus());
        Assert.assertEquals(201, response.getStatus());

        // get the item back but with "id" and "name" fields only
        Map<String, Object> value = decorateRequest(target.path("testItemToFilter")
                .queryParam("fields", "name")
                .request(MediaType.APPLICATION_JSON))
                .get(new GenericType<Map<String, Object>>() {
                });
        System.out.println("item: " + value);
        Assert.assertNotNull(value);
        Assert.assertNotNull(value.get("id"));
        Assert.assertNotNull(value.get("name"));
        Assert.assertNull(value.get("age"));    // other fields are not set

        // get the item back but with "id" fields only
        value = decorateRequest(target.path("testItemToFilter")
                .queryParam("fields", "")
                .request(MediaType.APPLICATION_JSON))
                .get(new GenericType<Map<String, Object>>() {
                });
        System.out.println("item: " + value);
        Assert.assertNotNull(value);
        Assert.assertNotNull(value.get("id"));
        Assert.assertNull(value.get("name"));
        Assert.assertNull(value.get("age"));
        Assert.assertEquals(1, value.size());   // only one field - id is set
    }

    @Test
    public void getSingleItemFromListTest() throws Exception {
        ObjectItem item1 = new ObjectItem();
        item1.setName("Test Name 1");
        item1.setAge(72);
        ObjectItem item2 = new ObjectItem();
        item2.setName("Test Name 2");
        item2.setAge(27);
        ObjectItem item3 = new ObjectItem();
        item3.setName("Test Name 3");
        item3.setAge(69);

        WebTarget target = target("testDB");

        // clean up
        Response response = decorateRequest(target.path("testItemsList").request()).delete();
//        Assert.assertEquals(200, response.getStatus());

        //put several objects to create a list
        response = decorateRequest(target.path("testItemsList").request())
                .post(Entity.entity(item1, MediaType.APPLICATION_JSON));
        response = decorateRequest(target.path("testItemsList").request())
                .post(Entity.entity(item2, MediaType.APPLICATION_JSON));
        response = decorateRequest(target.path("testItemsList").request())
                .post(Entity.entity(item3, MediaType.APPLICATION_JSON));

        // get the item back but with "id" and "name" fields only
        List<ObjectItem> value = decorateRequest(target.path("testItemsList")
                .request(MediaType.APPLICATION_JSON))
                .get(new GenericType<List<ObjectItem>>() {
                });
        System.out.println("items: " + value);
        Assert.assertNotNull(value);
        Assert.assertEquals(3, value.size());

        ObjectItem objectItem2 = value.get(1);
        ObjectItem objectItem3 = value.get(2);

        // get a single item by it's id
        ObjectItem item = decorateRequest(target.path("testItemsList/" + objectItem2.getId())
                .request(MediaType.APPLICATION_JSON))
                .get(new GenericType<ObjectItem>() {
                });
        System.out.println("item: " + item);
        Assert.assertNotNull(item);
        Assert.assertEquals(objectItem2.getId(), item.getId());

        // get another single item with a different request
        item = decorateRequest(target.path("testItemsList")
                .queryParam("id", objectItem3.getId())
                .request(MediaType.APPLICATION_JSON))
                .get(new GenericType<ObjectItem>() {
                });
        System.out.println("item: " + item);
        Assert.assertNotNull(item);
        Assert.assertEquals(objectItem3.getId(), item.getId());
    }

    @Test
    public void patchItemTest() throws Exception {
        WebTarget target = target("testDB");

        //set new resource at /testDB/testObject7
        ObjectItem item1 = new ObjectItem();
        item1.setName("Test Name 1");
        item1.setAge(69);
        target.property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true);
        Response response = decorateRequest(target.path("testObject7").request())
                .method("PATCH", Entity.entity(item1, MediaType.APPLICATION_JSON));
        System.out.println(response.getHeaderString("Location"));
        System.out.println(response.getStatus());
        Assert.assertEquals(201, response.getStatus());

        // get it back
        ObjectItem value = decorateRequest(target.path("testObject7").request(MediaType.APPLICATION_JSON))
                .get(new GenericType<ObjectItem>() {
                });
        System.out.println("item: " + value);
        Assert.assertNotNull(value);

        // update the object
        value.setName("Test Name 1 - updated");
        target.property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true);
        response = decorateRequest(target.path("testObject7").request())
                .method("PATCH", Entity.entity(value, MediaType.APPLICATION_JSON));
        System.out.println(response.getStatus());
        Assert.assertEquals(200, response.getStatus());

        // get it back again
        value = decorateRequest(target.path("testObject7").request(MediaType.APPLICATION_JSON))
                .get(new GenericType<ObjectItem>() {
                });
        System.out.println("item: " + value);
        Assert.assertNotNull(value);
        Assert.assertEquals("Test Name 1 - updated", value.getName());
    }

    @Test
    public void patchItemInListTest() throws Exception {
        WebTarget target = target("testDB");

        //set new resource at /testDB/testObject7
        ObjectItem item1 = new ObjectItem();
        item1.setName("Test Name 1");
        item1.setAge(69);
        ObjectItem item2 = new ObjectItem();
        item2.setName("Test Name 2");
        item2.setAge(123);
        ObjectItem item3 = new ObjectItem();
        item3.setId(123456L);        // set custom id
        item3.setName("Test Name 3");
        item3.setAge(12345);
        Response response = decorateRequest(target.path("testObject8").request())
                .put(Entity.entity(new ObjectItem[]{item1, item2, item3}, MediaType.APPLICATION_JSON));
        System.out.println(response.getHeaderString("Location"));
        System.out.println(response.getStatus());
        Assert.assertEquals(201, response.getStatus());

        // get it back
        List<ObjectItem> values = decorateRequest(target.path("testObject8").request(MediaType.APPLICATION_JSON))
                .get(new GenericType<List<ObjectItem>>() {
                });
        Assert.assertNotNull(values);
        Assert.assertEquals(3, values.size());
        ObjectItem value = values.get(1);

        // update the object
        value.setName("Test Name 1 - updated");
        target.property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true);
        response = decorateRequest(target.path("testObject8").request())
                .method("PATCH", Entity.entity(value, MediaType.APPLICATION_JSON));
        System.out.println(response.getStatus());
        Assert.assertEquals(200, response.getStatus());

        // get it back again
        values = decorateRequest(target.path("testObject8").request(MediaType.APPLICATION_JSON))
                .get(new GenericType<List<ObjectItem>>() {
                });
        Assert.assertNotNull(value);
        Assert.assertEquals(3, values.size());
        System.out.println("item: " + values.get(1));
        Assert.assertEquals("Test Name 1 - updated", values.get(1).getName());

        // update custom id object
        item3.setName("Test Name 3 - updated");
        item3.setAge(88);
        target.property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true);
        response = decorateRequest(target.path("testObject8").request())
                .method("PATCH", Entity.entity(item3, MediaType.APPLICATION_JSON));
        System.out.println(response.getStatus());
        Assert.assertEquals(200, response.getStatus());

        // get it all back
        values = decorateRequest(target.path("testObject8").request(MediaType.APPLICATION_JSON))
                .get(new GenericType<List<ObjectItem>>() {
                });
        Assert.assertNotNull(value);
        Assert.assertEquals(3, values.size());
        System.out.println("item: " + values.get(2));
        Assert.assertEquals(88, values.get(2).getAge().intValue());
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
    private Long id;
    private String name;
    private Integer age;
}