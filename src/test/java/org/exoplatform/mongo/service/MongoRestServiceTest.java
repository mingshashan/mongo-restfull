/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.mongo.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.bson.types.ObjectId;
import org.exoplatform.mongo.context.AppContext;
import org.exoplatform.mongo.factory.ResourceClientFactory;
import org.exoplatform.mongo.util.EncodingUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.webapp.WebAppContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.util.JSON;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;

/**
 * @author <a href="mailto:sondn@exoplatform.com">Ngoc Son Dang</a>
 * @since Jul 13, 2013
 * @version 
 * 
 * @tag 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({ "/META-INF/spring-local.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class MongoRestServiceTest {

	private static final String baseUri = "http://localhost:9002/api/mongo";
    private static final String baseDbUri = baseUri + "/databases";
    
	private Server server;
    private Client clientHandle;
    
    
    @BeforeClass
    public static void useTestOverrides() {
        System.setProperty("db.name", "test");
        System.setProperty("db.user", "");
        System.setProperty("db.password", "");
        System.setProperty("datastore.replicas", "");
        System.setProperty("domain", "localhost:9002");
    }

    @Before
    public void setUp() throws Exception {
        WebAppContext context = new WebAppContext();
        context.setContextPath("/");
        context.setWar(getWebappDirectory().getAbsolutePath());

        server = new Server(9002);
        server.setHandler(context);
        server.setStopAtShutdown(true);
        server.start();
        
        WebApplicationContextUtils.getWebApplicationContext(context.getServletContext());

        String testUsername = "junit";
        String testPassword = EncodingUtils.encodeBase64("r3$tfuLM0ng0");
        Mongo mongo = (Mongo) AppContext.getApplicationContext().getBean("mongo");
        DB db = mongo.getDB("credentials");
        DBCollection collection = db.getCollection("data_service");
        DBObject credentials = new BasicDBObject();
        credentials.put("user", testUsername);
        credentials.put("password", testPassword);
        collection.insert(credentials);

        clientHandle = ResourceClientFactory.getClientHandle(testUsername, testPassword, 0, 0);
    }

    // Adjust this if needed
    private File getWebappDirectory() {
        File file = new File("./src/main/webapp/");
        if (!file.exists()) {
            file = new File("~/mongo-restfull/src/main/webapp");
        }
        
        return file;
    }
    
    @After
    public void tearDown() throws Exception {
        try {
            if (server != null) {
                server.stop();
                server.destroy();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        Mongo mongo = (Mongo) AppContext.getApplicationContext().getBean("mongo");
        for (String dbName : mongo.getDatabaseNames()) {
            mongo.dropDatabase(dbName);
        }
    }

    @Test
    public void testCreateDatabase() {
    	org.exoplatform.mongo.entity.request.Database db = new org.exoplatform.mongo.entity.request.Database();
        db.setName("mongo-restfull");
        ClientResponse response = clientHandle.resource(baseDbUri).type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, db);
        System.out.println("-------------------------------------------------------");
        System.out.println("C R E A T E D " + Status.CREATED.getStatusCode() + "/" + response.getStatus());
        System.out.println("-------------------------------------------------------");
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
    }
    
    @Test
    public void testFindDatabase() {
    	org.exoplatform.mongo.entity.request.Database db = new org.exoplatform.mongo.entity.request.Database();
        String dbName = "mongo-restfull";
        db.setName(dbName);
        db.setWriteConcern(org.exoplatform.mongo.entity.request.WriteConcern.SAFE);
        ClientResponse response = clientHandle.resource(baseDbUri).type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, db);
        
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        response = clientHandle.resource(response.getLocation()).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        org.exoplatform.mongo.entity.response.Database database = response.getEntity(org.exoplatform.mongo.entity.response.Database.class);
        
        assertNotNull(database);
        assertEquals(dbName, database.getName());
        assertEquals(org.exoplatform.mongo.entity.response.WriteConcern.SAFE, database.getWriteConcern());
    }
    
    @Test
    public void testUpdateDatabase() {
        org.exoplatform.mongo.entity.request.Database db = new org.exoplatform.mongo.entity.request.Database();
        String dbName = "mongo-restfull";
        db.setName(dbName);
        db.setWriteConcern(org.exoplatform.mongo.entity.request.WriteConcern.SAFE);
        ClientResponse response = clientHandle.resource(baseDbUri).type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, db);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        URI dbUrl = response.getLocation();

        response = clientHandle.resource(dbUrl).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        org.exoplatform.mongo.entity.response.Database database = response
                .getEntity(org.exoplatform.mongo.entity.response.Database.class);
        assertNotNull(database);
        assertEquals(dbName, database.getName());
        assertEquals(org.exoplatform.mongo.entity.response.WriteConcern.SAFE, database.getWriteConcern());

        db.setWriteConcern(org.exoplatform.mongo.entity.request.WriteConcern.FSYNC_SAFE);
        response = clientHandle.resource(dbUrl).type(MediaType.APPLICATION_JSON_TYPE).put(ClientResponse.class, db);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        response = clientHandle.resource(dbUrl).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        database = response.getEntity(org.exoplatform.mongo.entity.response.Database.class);
        assertNotNull(database);
        assertEquals(dbName, database.getName());
        assertEquals(org.exoplatform.mongo.entity.response.WriteConcern.FSYNC_SAFE, database.getWriteConcern());
    }

    @Test
    public void testDeleteDatabase() {
        org.exoplatform.mongo.entity.request.Database db = new org.exoplatform.mongo.entity.request.Database();
        String dbName = "mongo-restfull";
        db.setName(dbName);
        ClientResponse response = clientHandle.resource(baseDbUri).type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, db);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        response = clientHandle.resource(response.getLocation()).delete(ClientResponse.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testFindDatabases() {
        org.exoplatform.mongo.entity.request.Database db = new org.exoplatform.mongo.entity.request.Database();
        String dbName1 = "mongo-restfull1";
        db.setName(dbName1);
        ClientResponse response = clientHandle.resource(baseDbUri).type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, db);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        db = new org.exoplatform.mongo.entity.request.Database();
        String dbName2 = "mongo-restfull2";
        db.setName(dbName2);
        response = clientHandle.resource(baseDbUri).type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, db);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        List<org.exoplatform.mongo.entity.response.Database> databases = clientHandle.resource(baseDbUri)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .get(new GenericType<List<org.exoplatform.mongo.entity.response.Database>>() {
                });
        assertEquals(2, databases.size());

        List<String> names = new ArrayList<String>(2);
        names.add(dbName1);
        names.add(dbName2);
        for (org.exoplatform.mongo.entity.response.Database database : databases) {
            assertTrue(names.contains(database.getName()));
            assertEquals(org.exoplatform.mongo.entity.response.WriteConcern.FSYNC_SAFE, database.getWriteConcern());
        }
    }

    @Test
    public void testDeleteDatabases() {
        org.exoplatform.mongo.entity.request.Database db = new org.exoplatform.mongo.entity.request.Database();
        String dbName1 = "mongo-restfull1";
        db.setName(dbName1);
        ClientResponse response = clientHandle.resource(baseDbUri).type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, db);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        db = new org.exoplatform.mongo.entity.request.Database();
        String dbName2 = "mongo-restfull2";
        db.setName(dbName2);
        response = clientHandle.resource(baseDbUri).type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, db);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        List<org.exoplatform.mongo.entity.response.Database> databases = clientHandle.resource(baseDbUri)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .get(new GenericType<List<org.exoplatform.mongo.entity.response.Database>>() {
                });
        assertEquals(2, databases.size());

        response = clientHandle.resource(baseDbUri).delete(ClientResponse.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        databases = clientHandle.resource(baseDbUri).type(MediaType.APPLICATION_JSON_TYPE)
                .get(new GenericType<List<org.exoplatform.mongo.entity.response.Database>>() {
                });
        assertEquals(0, databases.size());
    }

    @Test
    public void testCreateCollection() {
        org.exoplatform.mongo.entity.request.Database db = new org.exoplatform.mongo.entity.request.Database();
        db.setName("mongo-restfull");
        ClientResponse response = clientHandle.resource(baseDbUri).type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, db);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        URI dbUrl = response.getLocation();
        org.exoplatform.mongo.entity.request.Collection collection = new org.exoplatform.mongo.entity.request.Collection();
        collection.setName("mongo-collection-1");
        response = clientHandle.resource(dbUrl + "/collections").type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, collection);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        collection = new org.exoplatform.mongo.entity.request.Collection();
        collection.setName("mongo-collection-2");
        response = clientHandle.resource(dbUrl + "/collections").type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, collection);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        org.exoplatform.mongo.entity.response.Database database = clientHandle.resource(dbUrl)
                .queryParam("collDetails", "true").type(MediaType.APPLICATION_JSON_TYPE)
                .get(org.exoplatform.mongo.entity.response.Database.class);
        assertEquals(2, database.getCollections().size());

        for (org.exoplatform.mongo.entity.response.Collection coll : database.getCollections()) {
            assertEquals(1, coll.getIndexes().size());
            assertEquals(0, coll.getDocuments().size());
        }
    }

    @Test
    public void testFindCollection() {
        org.exoplatform.mongo.entity.request.Database db = new org.exoplatform.mongo.entity.request.Database();
        String dbName = "mongo-restfull";
        db.setName(dbName);
        ClientResponse response = clientHandle.resource(baseDbUri).type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, db);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        URI dbUrl = response.getLocation();
        org.exoplatform.mongo.entity.request.Collection collection = new org.exoplatform.mongo.entity.request.Collection();
        String collName = "mongo-test-collection";
        collection.setName(collName);
        response = clientHandle.resource(dbUrl + "/collections").type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, collection);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        org.exoplatform.mongo.entity.response.Collection foundCollection = clientHandle.resource(response.getLocation())
                .type(MediaType.APPLICATION_JSON_TYPE).get(org.exoplatform.mongo.entity.response.Collection.class);
        assertEquals(1, foundCollection.getIndexes().size());
        assertEquals(0, foundCollection.getDocuments().size());
        assertEquals(collName, foundCollection.getName());
        assertEquals(dbName, foundCollection.getDbName());
    }

    @Test
    public void testUpdateCollection() {
        org.exoplatform.mongo.entity.request.Database db = new org.exoplatform.mongo.entity.request.Database();
        String dbName = "mongo-restfull";
        db.setName(dbName);
        ClientResponse response = clientHandle.resource(baseDbUri).type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, db);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        URI dbUrl = response.getLocation();
        org.exoplatform.mongo.entity.request.Collection collection = new org.exoplatform.mongo.entity.request.Collection();
        String collName = "mongo-test-collection";
        collection.setName(collName);
        response = clientHandle.resource(dbUrl + "/collections").type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, collection);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        URI collUrl = response.getLocation();
        org.exoplatform.mongo.entity.response.Collection foundCollection = clientHandle.resource(collUrl)
                .type(MediaType.APPLICATION_JSON_TYPE).get(org.exoplatform.mongo.entity.response.Collection.class);
        assertEquals(1, foundCollection.getIndexes().size());
        assertEquals(0, foundCollection.getDocuments().size());
        assertEquals(collName, foundCollection.getName());
        assertEquals(dbName, foundCollection.getDbName());

        collection.setWriteConcern(org.exoplatform.mongo.entity.request.WriteConcern.FSYNC_SAFE);
        response = clientHandle.resource(collUrl).type(MediaType.APPLICATION_JSON_TYPE)
                .put(ClientResponse.class, collection);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        foundCollection = clientHandle.resource(collUrl).type(MediaType.APPLICATION_JSON_TYPE)
                .get(org.exoplatform.mongo.entity.response.Collection.class);
        assertEquals(1, foundCollection.getIndexes().size());
        assertEquals(0, foundCollection.getDocuments().size());
        assertEquals(collName, foundCollection.getName());
        assertEquals(dbName, foundCollection.getDbName());
        assertEquals(org.exoplatform.mongo.entity.response.WriteConcern.FSYNC_SAFE, foundCollection.getWriteConcern());
    }

    @Test
    public void testDeleteCollection() {
        org.exoplatform.mongo.entity.request.Database db = new org.exoplatform.mongo.entity.request.Database();
        String dbName = "mongo-restfull";
        db.setName(dbName);
        ClientResponse response = clientHandle.resource(baseDbUri).type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, db);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        URI dbUrl = response.getLocation();
        org.exoplatform.mongo.entity.request.Collection collection = new org.exoplatform.mongo.entity.request.Collection();
        String collName = "mongo-test-collection";
        collection.setName(collName);
        response = clientHandle.resource(dbUrl + "/collections").type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, collection);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        URI collUrl = response.getLocation();
        response = clientHandle.resource(collUrl).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        response = clientHandle.resource(collUrl).delete(ClientResponse.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        response = clientHandle.resource(collUrl).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testFindCollections() {
        org.exoplatform.mongo.entity.request.Database db = new org.exoplatform.mongo.entity.request.Database();
        String dbName = "mongo-restfull";
        db.setName(dbName);
        ClientResponse response = clientHandle.resource(baseDbUri).type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, db);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        URI dbUrl = response.getLocation();
        org.exoplatform.mongo.entity.request.Collection collection = new org.exoplatform.mongo.entity.request.Collection();
        String collName = "mongo-test-collection-1";
        collection.setName(collName);
        response = clientHandle.resource(dbUrl + "/collections").type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, collection);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        URI collUrl1 = response.getLocation();
        response = clientHandle.resource(collUrl1).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        collection = new org.exoplatform.mongo.entity.request.Collection();
        collName = "mongo-test-collection-2";
        collection.setName(collName);
        response = clientHandle.resource(dbUrl + "/collections").type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, collection);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        URI collUrl2 = response.getLocation();
        response = clientHandle.resource(collUrl2).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        List<org.exoplatform.mongo.entity.response.Collection> collections = clientHandle.resource(dbUrl + "/collections")
                .type(MediaType.APPLICATION_JSON_TYPE)
                .get(new GenericType<List<org.exoplatform.mongo.entity.response.Collection>>() {
                });
        assertEquals(2, collections.size());
    }

    @Test
    public void testDeleteCollections() {
        org.exoplatform.mongo.entity.request.Database db = new org.exoplatform.mongo.entity.request.Database();
        String dbName = "mongo-restfull";
        db.setName(dbName);
        ClientResponse response = clientHandle.resource(baseDbUri).type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, db);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        URI dbUrl = response.getLocation();
        org.exoplatform.mongo.entity.request.Collection collection = new org.exoplatform.mongo.entity.request.Collection();
        String collName = "mongo-test-collection-1";
        collection.setName(collName);
        response = clientHandle.resource(dbUrl + "/collections").type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, collection);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        URI collUrl1 = response.getLocation();
        response = clientHandle.resource(collUrl1).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        collection = new org.exoplatform.mongo.entity.request.Collection();
        collName = "mongo-test-collection-2";
        collection.setName(collName);
        response = clientHandle.resource(dbUrl + "/collections").type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, collection);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        URI collUrl2 = response.getLocation();
        response = clientHandle.resource(collUrl2).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        response = clientHandle.resource(dbUrl + "/collections").delete(ClientResponse.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        response = clientHandle.resource(collUrl1).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        response = clientHandle.resource(collUrl2).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCreateIndex() {
        org.exoplatform.mongo.entity.request.Database db = new org.exoplatform.mongo.entity.request.Database();
        String dbName = "mongo-restfull";
        db.setName(dbName);
        ClientResponse response = clientHandle.resource(baseDbUri).type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, db);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        URI dbUrl = response.getLocation();
        org.exoplatform.mongo.entity.request.Collection collection = new org.exoplatform.mongo.entity.request.Collection();
        String collName = "mongo-test-collection";
        collection.setName(collName);
        response = clientHandle.resource(dbUrl + "/collections").type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, collection);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        URI collUrl = response.getLocation();
        response = clientHandle.resource(collUrl).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        org.exoplatform.mongo.entity.request.Index index = new org.exoplatform.mongo.entity.request.Index();
        index.setName("simple-index");
        index.setUnique(true);
        List<String> keys = new ArrayList<String>();
        keys.add("name");
        keys.add("age");
        index.setKeys(keys);
        response = clientHandle.resource(collUrl + "/indexes").type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, index);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
    }

    @Test
    public void testFindIndex() {
        org.exoplatform.mongo.entity.request.Database db = new org.exoplatform.mongo.entity.request.Database();
        String dbName = "mongo-restfull";
        db.setName(dbName);
        ClientResponse response = clientHandle.resource(baseDbUri).type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, db);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        URI dbUrl = response.getLocation();
        org.exoplatform.mongo.entity.request.Collection collection = new org.exoplatform.mongo.entity.request.Collection();
        String collName = "mongo-test-collection";
        collection.setName(collName);
        response = clientHandle.resource(dbUrl + "/collections").type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, collection);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        URI collUrl = response.getLocation();
        response = clientHandle.resource(collUrl).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        org.exoplatform.mongo.entity.request.Index index1 = new org.exoplatform.mongo.entity.request.Index();
        index1.setName("stats-index");
        index1.setUnique(true);
        List<String> keys = new ArrayList<String>();
        keys.add("name");
        keys.add("age");
        keys.add("height");
        index1.setKeys(keys);
        response = clientHandle.resource(collUrl + "/indexes").type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, index1);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        URI index1Url = response.getLocation();

        response = clientHandle.resource(index1Url).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        org.exoplatform.mongo.entity.response.Index foundIndex = response
                .getEntity(org.exoplatform.mongo.entity.response.Index.class);
        assertNotNull(foundIndex);
        assertEquals(index1.getName(), foundIndex.getName());
        for (String key : index1.getKeys()) {
            assertTrue(foundIndex.getKeys().contains(key));
        }
        assertEquals(index1.isUnique(), foundIndex.isUnique());
    }

    @Test
    public void testDeleteIndex() {
        org.exoplatform.mongo.entity.request.Database db = new org.exoplatform.mongo.entity.request.Database();
        String dbName = "mongo-restfull";
        db.setName(dbName);
        ClientResponse response = clientHandle.resource(baseDbUri).type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, db);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        URI dbUrl = response.getLocation();
        org.exoplatform.mongo.entity.request.Collection collection = new org.exoplatform.mongo.entity.request.Collection();
        String collName = "mongo-test-collection";
        collection.setName(collName);
        response = clientHandle.resource(dbUrl + "/collections").type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, collection);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        URI collUrl = response.getLocation();
        response = clientHandle.resource(collUrl).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        org.exoplatform.mongo.entity.request.Index index = new org.exoplatform.mongo.entity.request.Index();
        index.setName("stats-index");
        index.setUnique(true);
        List<String> keys = new ArrayList<String>();
        keys.add("name");
        keys.add("age");
        keys.add("height");
        index.setKeys(keys);
        response = clientHandle.resource(collUrl + "/indexes").type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, index);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        URI indexUrl = response.getLocation();

        response = clientHandle.resource(indexUrl).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        response = clientHandle.resource(indexUrl).delete(ClientResponse.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        response = clientHandle.resource(indexUrl).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testFindIndexes() {
        org.exoplatform.mongo.entity.request.Database db = new org.exoplatform.mongo.entity.request.Database();
        String dbName = "mongo-restfull";
        db.setName(dbName);
        ClientResponse response = clientHandle.resource(baseDbUri).type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, db);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        URI dbUrl = response.getLocation();
        org.exoplatform.mongo.entity.request.Collection collection = new org.exoplatform.mongo.entity.request.Collection();
        String collName = "mongo-test-collection";
        collection.setName(collName);
        response = clientHandle.resource(dbUrl + "/collections").type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, collection);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        URI collUrl = response.getLocation();
        response = clientHandle.resource(collUrl).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        org.exoplatform.mongo.entity.request.Index index1 = new org.exoplatform.mongo.entity.request.Index();
        index1.setName("stats-index");
        index1.setUnique(true);
        List<String> keys = new ArrayList<String>();
        keys.add("name");
        keys.add("age");
        keys.add("height");
        index1.setKeys(keys);
        response = clientHandle.resource(collUrl + "/indexes").type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, index1);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        org.exoplatform.mongo.entity.request.Index index2 = new org.exoplatform.mongo.entity.request.Index();
        index2.setName("location-index");
        index2.setUnique(true);
        keys = new ArrayList<String>();
        keys.add("address");
        keys.add("phone");
        index2.setKeys(keys);
        response = clientHandle.resource(collUrl + "/indexes").type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, index2);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        List<org.exoplatform.mongo.entity.request.Index> indexes = new ArrayList<org.exoplatform.mongo.entity.request.Index>(2);
        indexes.add(index1);
        indexes.add(index2);

        response = clientHandle.resource(collUrl + "/indexes").type(MediaType.APPLICATION_JSON_TYPE)
                .get(ClientResponse.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        List<org.exoplatform.mongo.entity.response.Index> foundIndexes = response
                .getEntity(new GenericType<List<org.exoplatform.mongo.entity.response.Index>>() {
                });
        assertEquals(2, foundIndexes.size());
        for (org.exoplatform.mongo.entity.response.Index foundIndex : foundIndexes) {
            for (org.exoplatform.mongo.entity.request.Index index : indexes) {
                if (index.getName().equals(foundIndex.getName())) {
                    for (String key : index.getKeys()) {
                        assertTrue(foundIndex.getKeys().contains(key));
                    }
                    assertEquals(index.isUnique(), foundIndex.isUnique());
                    assertEquals(dbName, foundIndex.getDbName());
                    assertEquals(collName, foundIndex.getCollectionName());
                }
            }
        }
    }

    @Test
    public void testDeleteIndexes() {
        org.exoplatform.mongo.entity.request.Database db = new org.exoplatform.mongo.entity.request.Database();
        String dbName = "mongo-restfull";
        db.setName(dbName);
        ClientResponse response = clientHandle.resource(baseDbUri).type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, db);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        URI dbUrl = response.getLocation();
        org.exoplatform.mongo.entity.request.Collection collection = new org.exoplatform.mongo.entity.request.Collection();
        String collName = "mongo-test-collection";
        collection.setName(collName);
        response = clientHandle.resource(dbUrl + "/collections").type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, collection);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        URI collUrl = response.getLocation();
        response = clientHandle.resource(collUrl).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        org.exoplatform.mongo.entity.request.Index index = new org.exoplatform.mongo.entity.request.Index();
        index.setName("stats-index");
        index.setUnique(true);
        List<String> keys = new ArrayList<String>();
        keys.add("name");
        keys.add("age");
        keys.add("height");
        index.setKeys(keys);
        response = clientHandle.resource(collUrl + "/indexes").type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, index);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        URI indexUrl = response.getLocation();

        response = clientHandle.resource(indexUrl).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        response = clientHandle.resource(collUrl + "/indexes").delete(ClientResponse.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        response = clientHandle.resource(indexUrl).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        response = clientHandle.resource(collUrl + "/indexes").type(MediaType.APPLICATION_JSON_TYPE)
                .get(ClientResponse.class);
        List<org.exoplatform.mongo.entity.response.Index> foundIndexes = response
                .getEntity(new GenericType<List<org.exoplatform.mongo.entity.response.Index>>() {
                });
        assertEquals(0, foundIndexes.size());
    }

    @Test
    public void testCreateDocument() {
        org.exoplatform.mongo.entity.request.Database db = new org.exoplatform.mongo.entity.request.Database();
        String dbName = "mongo-restfull";
        db.setName(dbName);
        ClientResponse response = clientHandle.resource(baseDbUri).type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, db);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        URI dbUrl = response.getLocation();
        org.exoplatform.mongo.entity.request.Collection collection = new org.exoplatform.mongo.entity.request.Collection();
        String collName = "mongo-test-collection";
        collection.setName(collName);
        response = clientHandle.resource(dbUrl + "/collections").type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, collection);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        URI collUrl = response.getLocation();
        response = clientHandle.resource(collUrl).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        Map<String, Object> objMap = new HashMap<String, Object>();
        String fKey = "city";
        String fValue = "san francisco";
        objMap.put(fKey, fValue);
        String sKey = "state";
        String sValue = "california";
        objMap.put(sKey, sValue);
        Map<String, String> tempMap = new HashMap<String, String>();
        String jKey = "january";
        String jTemp = "50";
        tempMap.put(jKey, jTemp);
        String dKey = "december";
        String dTemp = "45";
        tempMap.put(dKey, dTemp);
        String tempMapKey = "tempMap";
        objMap.put(tempMapKey, tempMap);
        String json = JSON.serialize(objMap);
        org.exoplatform.mongo.entity.request.Document document = new org.exoplatform.mongo.entity.request.Document();
        document.setJson(json);

        response = clientHandle.resource(collUrl + "/documents").type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, document);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
    }

    @Test
    public void testFindDocument() {
        org.exoplatform.mongo.entity.request.Database db = new org.exoplatform.mongo.entity.request.Database();
        String dbName = "mongo-restfull";
        db.setName(dbName);
        ClientResponse response = clientHandle.resource(baseDbUri).type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, db);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        URI dbUrl = response.getLocation();
        org.exoplatform.mongo.entity.request.Collection collection = new org.exoplatform.mongo.entity.request.Collection();
        String collName = "mongo-test-collection";
        collection.setName(collName);
        response = clientHandle.resource(dbUrl + "/collections").type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, collection);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        URI collUrl = response.getLocation();
        response = clientHandle.resource(collUrl).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        Map<String, Object> objMap = new HashMap<String, Object>();
        String fKey = "city";
        String fValue = "san francisco";
        objMap.put(fKey, fValue);
        String sKey = "state";
        String sValue = "california";
        objMap.put(sKey, sValue);
        Map<String, String> tempMap = new HashMap<String, String>();
        String jKey = "january";
        String jTemp = "50";
        tempMap.put(jKey, jTemp);
        String dKey = "december";
        String dTemp = "45";
        tempMap.put(dKey, dTemp);
        String tempMapKey = "tempMap";
        objMap.put(tempMapKey, tempMap);
        String json = JSON.serialize(objMap);
        org.exoplatform.mongo.entity.request.Document document = new org.exoplatform.mongo.entity.request.Document();
        document.setJson(json);

        response = clientHandle.resource(collUrl + "/documents").type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, document);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        response = clientHandle.resource(response.getLocation()).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        org.exoplatform.mongo.entity.response.Document insertedDocument = response.getEntity(org.exoplatform.mongo.entity.response.Document.class);
        BasicDBObject foundJson = (BasicDBObject) JSON.parse(insertedDocument.getJson());
        @SuppressWarnings("rawtypes")
        Map foundMap = foundJson.toMap();
        for (String key : objMap.keySet()) {
            assertTrue(foundMap.containsKey(key));
            assertEquals(objMap.get(key), foundMap.get(key));
        }
        assertNotNull(foundMap.get("_id"));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testUpdateDocument() {
        org.exoplatform.mongo.entity.request.Database db = new org.exoplatform.mongo.entity.request.Database();
        String dbName = "mongo-restfull";
        db.setName(dbName);
        ClientResponse response = clientHandle.resource(baseDbUri).type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, db);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        URI dbUrl = response.getLocation();
        org.exoplatform.mongo.entity.request.Collection collection = new org.exoplatform.mongo.entity.request.Collection();
        String collName = "mongo-test-collection";
        collection.setName(collName);
        response = clientHandle.resource(dbUrl + "/collections").type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, collection);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        URI collUrl = response.getLocation();
        response = clientHandle.resource(collUrl).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        Map<String, Object> objMap = new HashMap<String, Object>();
        String fKey = "city";
        String fValue = "san francisco";
        objMap.put(fKey, fValue);
        String sKey = "state";
        String sValue = "california";
        objMap.put(sKey, sValue);
        Map<String, String> tempMap = new HashMap<String, String>();
        String jKey = "january";
        String jTemp = "50";
        tempMap.put(jKey, jTemp);
        String dKey = "december";
        String dTemp = "45";
        tempMap.put(dKey, dTemp);
        String tempMapKey = "tempMap";
        objMap.put(tempMapKey, tempMap);
        String json = JSON.serialize(objMap);
        org.exoplatform.mongo.entity.request.Document document = new org.exoplatform.mongo.entity.request.Document();
        document.setJson(json);

        response = clientHandle.resource(collUrl + "/documents").type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, document);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        URI docUrl = response.getLocation();

        response = clientHandle.resource(docUrl).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        org.exoplatform.mongo.entity.response.Document insertedDocument = response.getEntity(org.exoplatform.mongo.entity.response.Document.class);

        BasicDBObject insertedJson = (BasicDBObject) JSON.parse(insertedDocument.getJson());
        Map insertedMap = insertedJson.toMap();
        for (String key : objMap.keySet()) {
            assertTrue(insertedMap.containsKey(key));
            assertEquals(objMap.get(key), insertedMap.get(key));
        }
        ObjectId docId = (ObjectId) insertedMap.get("_id");
        assertNotNull(docId);

        fKey = "city";
        fValue = "fresno";
        insertedMap.put(fKey, fValue);
        document.setJson(JSON.serialize(insertedMap));

        response = clientHandle.resource(docUrl).type(MediaType.APPLICATION_JSON_TYPE).put(ClientResponse.class, document);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        response = clientHandle.resource(docUrl).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        org.exoplatform.mongo.entity.response.Document updatedDocument = response.getEntity(org.exoplatform.mongo.entity.response.Document.class);

        BasicDBObject updatedJson = (BasicDBObject) JSON.parse(updatedDocument.getJson());
        Map updatedMap = updatedJson.toMap();
        for (Object key : insertedMap.keySet()) {
            if (key.equals("_id")) {
                continue;
            }
            assertTrue(updatedMap.containsKey(key));
            assertEquals(insertedMap.get(key), updatedMap.get(key));
        }
        assertEquals(docId, (ObjectId) updatedMap.get("_id"));
    }

    @Test
    public void testDeleteDocument() {
        org.exoplatform.mongo.entity.request.Database db = new org.exoplatform.mongo.entity.request.Database();
        String dbName = "mongo-restfull";
        db.setName(dbName);
        ClientResponse response = clientHandle.resource(baseDbUri).type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, db);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        URI dbUrl = response.getLocation();
        org.exoplatform.mongo.entity.request.Collection collection = new org.exoplatform.mongo.entity.request.Collection();
        String collName = "mongo-test-collection";
        collection.setName(collName);
        response = clientHandle.resource(dbUrl + "/collections").type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, collection);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        URI collUrl = response.getLocation();
        response = clientHandle.resource(collUrl).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        Map<String, Object> objMap = new HashMap<String, Object>();
        String fKey = "city";
        String fValue = "san francisco";
        objMap.put(fKey, fValue);
        String sKey = "state";
        String sValue = "california";
        objMap.put(sKey, sValue);
        Map<String, String> tempMap = new HashMap<String, String>();
        String jKey = "january";
        String jTemp = "50";
        tempMap.put(jKey, jTemp);
        String dKey = "december";
        String dTemp = "45";
        tempMap.put(dKey, dTemp);
        String tempMapKey = "tempMap";
        objMap.put(tempMapKey, tempMap);
        String json = JSON.serialize(objMap);
        org.exoplatform.mongo.entity.request.Document document = new org.exoplatform.mongo.entity.request.Document();
        document.setJson(json);

        response = clientHandle.resource(collUrl + "/documents").type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, document);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        URI docUrl = response.getLocation();

        response = clientHandle.resource(docUrl).delete(ClientResponse.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        response = clientHandle.resource(docUrl).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testFindDocuments() {
        org.exoplatform.mongo.entity.request.Database db = new org.exoplatform.mongo.entity.request.Database();
        String dbName = "mongo-restfull";
        db.setName(dbName);
        ClientResponse response = clientHandle.resource(baseDbUri).type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, db);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        URI dbUrl = response.getLocation();
        org.exoplatform.mongo.entity.request.Collection collection = new org.exoplatform.mongo.entity.request.Collection();
        String collName = "mongo-test-collection";
        collection.setName(collName);
        response = clientHandle.resource(dbUrl + "/collections").type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, collection);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        URI collUrl = response.getLocation();
        response = clientHandle.resource(collUrl).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        Map<String, Object> objMap = new HashMap<String, Object>();
        String fKey = "city";
        String fValue = "san francisco";
        objMap.put(fKey, fValue);
        String sKey = "state";
        String sValue = "california";
        objMap.put(sKey, sValue);
        Map<String, String> tempMap = new HashMap<String, String>();
        String jKey = "january";
        String jTemp = "50";
        tempMap.put(jKey, jTemp);
        String dKey = "december";
        String dTemp = "45";
        tempMap.put(dKey, dTemp);
        String tempMapKey = "tempMap";
        objMap.put(tempMapKey, tempMap);
        String json = JSON.serialize(objMap);
        org.exoplatform.mongo.entity.request.Document document = new org.exoplatform.mongo.entity.request.Document();
        document.setJson(json);

        response = clientHandle.resource(collUrl + "/documents").type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, document);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        List<org.exoplatform.mongo.entity.response.Document> foundDocuments = clientHandle.resource(collUrl + "/documents")
                .type(MediaType.APPLICATION_JSON_TYPE).get(new GenericType<List<org.exoplatform.mongo.entity.response.Document>>() {});
        assertEquals(1, foundDocuments.size());
        BasicDBObject foundJson = (BasicDBObject) JSON.parse(foundDocuments.get(0).getJson());
        @SuppressWarnings("rawtypes")
        Map foundMap = foundJson.toMap();
        for (String key : objMap.keySet()) {
            assertTrue(foundMap.containsKey(key));
            assertEquals(objMap.get(key), foundMap.get(key));
        }
        assertNotNull(foundMap.get("_id"));
    }

    @Test
    public void testDeleteDocuments() {
        org.exoplatform.mongo.entity.request.Database db = new org.exoplatform.mongo.entity.request.Database();
        String dbName = "mongo-restfull";
        db.setName(dbName);
        ClientResponse response = clientHandle.resource(baseDbUri).type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, db);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        URI dbUrl = response.getLocation();
        org.exoplatform.mongo.entity.request.Collection collection = new org.exoplatform.mongo.entity.request.Collection();
        String collName = "mongo-test-collection";
        collection.setName(collName);
        response = clientHandle.resource(dbUrl + "/collections").type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, collection);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        URI collUrl = response.getLocation();
        response = clientHandle.resource(collUrl).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        Map<String, Object> objMap = new HashMap<String, Object>();
        String fKey = "city";
        String fValue = "san francisco";
        objMap.put(fKey, fValue);
        String sKey = "state";
        String sValue = "california";
        objMap.put(sKey, sValue);
        Map<String, String> tempMap = new HashMap<String, String>();
        String jKey = "january";
        String jTemp = "50";
        tempMap.put(jKey, jTemp);
        String dKey = "december";
        String dTemp = "45";
        tempMap.put(dKey, dTemp);
        String tempMapKey = "tempMap";
        objMap.put(tempMapKey, tempMap);
        String json = JSON.serialize(objMap);
        org.exoplatform.mongo.entity.request.Document document = new org.exoplatform.mongo.entity.request.Document();
        document.setJson(json);

        response = clientHandle.resource(collUrl + "/documents").type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, document);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        URI docUrl = response.getLocation();

        response = clientHandle.resource(collUrl + "/documents").delete(ClientResponse.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        response = clientHandle.resource(docUrl).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testPing() {
        ClientResponse response = clientHandle.resource(baseUri + "/ping").type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testShutdown() {
        ClientResponse response = clientHandle.resource(baseUri + "/ping").type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        response = clientHandle.resource(baseUri + "/shutdown").type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        response = clientHandle.resource(baseUri + "/ping").type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        assertEquals(Status.SERVICE_UNAVAILABLE.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCreateBinary() {
        org.exoplatform.mongo.entity.request.Database db = new org.exoplatform.mongo.entity.request.Database();
        String dbName = "mongo-restfull";
        db.setName(dbName);
        ClientResponse response = clientHandle.resource(baseDbUri).type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, db);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        URI dbUrl = response.getLocation();
        org.exoplatform.mongo.entity.request.Collection collection = new org.exoplatform.mongo.entity.request.Collection();
        String collName = "mongo-test-collection";
        collection.setName(collName);
        response = clientHandle.resource(dbUrl + "/collections").type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, collection);
        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        URI collUrl = response.getLocation();
        response = clientHandle.resource(collUrl).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }
}
