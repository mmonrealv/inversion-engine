/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion.cloud.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.inversion.cloud.action.misc.MockAction;
import io.inversion.cloud.model.Action;
import io.inversion.cloud.model.Api;
import io.inversion.cloud.model.Endpoint;
import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.model.MockActionA;
import io.inversion.cloud.model.MockActionB;
import io.inversion.cloud.model.MockDb;
import io.inversion.cloud.model.Path;
import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.utils.Utils;

public class EngineTest
{
   @Test
   public void configureRequest_version_and_tenant()
   {
      Api api1 = new Api("api1").withEndpoint("*", "ep1/*");
      Api api2 = new Api("api2");

      Engine e = new Engine(api1, api2);

      Request req = null;
      Response res = null;

      req = e.get("http://localhost:8080/api1/ep1").getChain().getRequest();
      assertTrue(req.getEndpoint() != null);
      assertEquals(new Path("ep1"), req.getEndpointPath());
      assertEquals(new Path("api1"), req.getApiPath());

      e.withServletMapping("/some/servlet/path");

      req = e.get("http://localhost:8080/some/servlet/path/api1/ep1").getChain().getRequest();
      assertTrue(req.getEndpoint() != null);
      assertEquals(new Path("ep1"), req.getEndpointPath());
      assertEquals(new Path("/some/servlet/path/api1"), req.getApiPath());

      api1.withVersion("v1");
      req = e.get("http://localhost:8080/some/servlet/path/api1/ep1").getChain().getRequest();
      assertTrue(req.getEndpoint() == null);

      req = e.get("http://localhost:8080/some/servlet/path/api1/v1/ep1").getChain().getRequest();
      assertTrue(req.getEndpoint() != null);
      assertEquals(new Path("ep1"), req.getEndpointPath());
      assertEquals(new Path("/some/servlet/path/api1/v1"), req.getApiPath());

      api1.withMultiTenant(true);
      req = e.get("http://localhost:8080/some/servlet/path/api1/v1/acme/ep1").getChain().getRequest();
      assertTrue(req.getEndpoint() != null);
      assertEquals(new Path("ep1"), req.getEndpointPath());
      assertEquals(new Path("/some/servlet/path/api1/v1/acme"), req.getApiPath());
      assertEquals("acme", req.getTenant());
   }

   @Test
   public void findApiForPath_selectApiVersion()
   {
      Engine e = new Engine().withApi(new Api("test").withVersion("v1"))//
                             .withApi(new Api("test").withVersion("v2"))//
                             .withApi(new Api("test2"))//
      ;

      assertNull(e.matchApi(Utils.explode("/", "")));
      assertNull(e.matchApi(Utils.explode("/", "test/")));
      assertNull(e.matchApi(Utils.explode("/", "test/blah")));

      assertEquals("v2", e.matchApi(Utils.explode("/", "test/v2")).getVersion());
      assertEquals("v2", e.matchApi(Utils.explode("/", "test/v2/collection")).getVersion());

      assertEquals("test2", e.matchApi(Utils.explode("/", "test2")).getName());
      assertEquals("test2", e.matchApi(Utils.explode("/", "test2/collection")).getName());
   }

   @Test
   public void testEndpointMatching2()
   {
      Engine engine = null;

      engine = new Engine()//
                           .withApi(new Api("northwind")//
                                                        .withEndpoint(null, "source/*", new MockAction("sourceAction"))//
                                                        .withEndpoint(null, "h2/*", new MockAction("h2Action"))//
                                                        .withEndpoint(null, "mysql/*", new MockAction("mysqlAction"))//
                                                        .withEndpoint(null, "dynamo/*", new MockAction("dynamoAction")));

      engine.get("northwind/source/collection").assertDebug("Action:", "sourceAction");
      engine.get("northwind/source").assertDebug("Action:", "sourceAction");
      engine.get("northwind/h2/collection").assertDebug("Actio:n", "h2Action");
      engine.get("northwind/mysql/collection/entity/subcollection").assertDebug("Action:", "mysqlAction");
      engine.get("northwind/dynamo/collection").assertDebug("Action:", "dynamoAction");
   }

   @Test
   public void test_endpoints_without_paths()
   {
      Api api = null;

      api = new Api("test")//
                           .withAction(new MockAction("mock1").withIncludePaths("*"))//
                           .withEndpoint(new Endpoint("GET", "*").withName("ep1").withExcludePaths("subpath/*"))//
                           .withEndpoint(new Endpoint("GET", "subpath/*").withName("ep2"))//
      ;

      assertEndpointMatch("GET", "http://localhost/test/colKey/entKey/relKey", 200, "ep1", "", "colKey", "entKey", "relKey", api);
      assertEndpointMatch("GET", "http://localhost/test/subpath/colKey/entKey/relKey", 200, "ep2", "subpath", "colKey", "entKey", "relKey", api);

      api = new Api("test")//
                           .withAction(new MockAction("mock1").withIncludePaths("*"))//
                           .withEndpoint(new Endpoint("GET", "/").withName("ep1").withIncludePaths("collection1/*,collection2/*"))//
                           .withEndpoint(new Endpoint("GET", "subpath3/*").withName("ep2"))//
      ;

      assertEndpointMatch("GET", "http://localhost/test/collection1/entKey/relKey", 200, "ep1", "", "collection1", "entKey", "relKey", api);
      assertEndpointMatch("GET", "http://localhost/test/collection2/entKey/relKey", 200, "ep1", "", "collection2", "entKey", "relKey", api);
      assertEndpointMatch("GET", "http://localhost/test/subpath3/colKey/entKey/relKey", 200, "ep2", "subpath3", "colKey", "entKey", "relKey", api);

   }

   @Test
   public void test_endpoint_matches()
   {
      Api api0 = new Api()//
                          .withEndpoint(new Endpoint("GET", "endpoint_path/*", new MockAction("all")).withName("ep0"));

      //if you only have one API, you can leave the API code null...you can only really do this from code configured APIs not prop wired APIs
      assertEndpointMatch("GET", "http://localhost/endpoint_path/12345", 200, "ep0", "endpoint_path", "12345", null, null, api0);

      Api api1 = new Api("test")//
                                .withAction(new MockAction("mock1").withIncludePaths("*"))//
                                .withEndpoint(new Endpoint("GET", "ep1/*").withName("ep1"))//
                                .withEndpoint(new Endpoint("GET", "ep2/").withName("ep2"))//
                                .withEndpoint(new Endpoint("GET", "bookstore/", "books/*,categories,authors").withName("ep3"))//
                                .withEndpoint(new Endpoint("GET", "other/data", "table1,table2/*,other/data/*,data/*").withName("ep4"))//
                                .withEndpoint(new Endpoint("GET", "cardealer", "ford/*,gm/*").withName("ep5"))//
                                .withEndpoint(new Endpoint("GET", "petstore/*").withName("ep6").withExcludePaths("rat", "snakes/bad", "cats/*"))//
                                .withEndpoint(new Endpoint("GET", "gamestop/*", "nintendo,xbox/*").withName("ep7"))//
                                .withEndpoint(new Endpoint("GET", "carwash", "regular,delux/*").withName("ep8"));

      Api api2 = new Api("other");

      assertEndpointMatch("GET", "http://localhost/test/ep1", 200, api1);
      assertEndpointMatch("GET", "/test/ep1", 200, api1);
      assertEndpointMatch("GET", "test/ep1", 200, api1);
      assertEndpointMatch("GET", "http://localhost/WRONG/ep1", 400, api1);
      assertEndpointMatch("GET", "http://localhost/WRONG/ep1", 400, api1, api2);

      assertEndpointMatch("GET", "http://localhost/test/ep1/collKey/entKey/relKey", 200, "ep1", "ep1", "collKey", "entKey", "relKey", api1);
      assertEndpointMatch("GET", "http://localhost/test/ep1/collKey/entKey/relKey", 500, "ep1", "ep1", "collKey", "entKey", "asdfasd", api1);
      assertEndpointMatch("DELETE", "http://localhost/test/ep1/collKey/entKey/relKey", 404, api1);

      assertEndpointMatch("GET", "test/ep2", 200, api1);
      assertEndpointMatch("GET", "http://localhost/test/ep2/", 200, api1);
      assertEndpointMatch("GET", "http://localhost/test/ep2/asdf", 404, api1);

      assertEndpointMatch("GET", "http://localhost/test/bookstore/books/1/author", 200, "ep3", "bookstore", "books", "1", "author", api1);
      assertEndpointMatch("GET", "http://localhost/test/bookstore/categories/fiction/books", 404, "ep3", "bookstore", "categories", "fiction", "books", api1);
      assertEndpointMatch("GET", "http://localhost/test/bookstore/cars/", 404, api1);

      assertEndpointMatch("GET", "/test/other/data/table1/", 200, "ep4", "other/data", "table1", null, null, api1);
      assertEndpointMatch("GET", "http://localhost/test/other/data/table1/asdfa/", 404, api1);
      assertEndpointMatch("GET", "http://localhost/test/other/data/table2/keyCol/relCol", 200, "ep4", "other/data", "table2", "keyCol", "relCol", api1);
      assertEndpointMatch("GET", "test/other/data/data/keyCol/relCol", 200, "ep4", "other/data", "data", "keyCol", "relCol", api1);
      assertEndpointMatch("GET", "http://localhost/test/other/data/other/data/relCol", 200, "ep4", "other/data", "other", "data", "relCol", api1);

      assertEndpointMatch("GET", "/test/cardealer/ford/explorer", 200, "ep5", "cardealer", "ford", "explorer", null, api1);
      assertEndpointMatch("GET", "/test/cardealer/gm", 200, "ep5", "cardealer", "gm", null, null, api1);
      assertEndpointMatch("GET", "/test/cardealer/ford/toyota", 400);

      assertEndpointMatch("GET", "/test/petstore/dogs/1234/breed", 200, "ep6", "petstore", "dogs", "1234", "breed", api1);
      assertEndpointMatch("GET", "/test/petstore/rat/", 404, api1);
      assertEndpointMatch("GET", "/test/petstore/rat/a_rat", 200, "ep6", "petstore", "rat", "a_rat", null, api1);
      assertEndpointMatch("GET", "/test/petstore/snakes/good", 200, "ep6", "petstore", "snakes", "good", null, api1);
      assertEndpointMatch("GET", "/test/petstore/snakes/bad/", 404, api1);
      assertEndpointMatch("GET", "/test/petstore/snakes/bad/butgood", 200, "ep6", "petstore", "snakes", "bad", "butgood", api1);
      assertEndpointMatch("GET", "/test/petstore/cats/", 404, api1);
      assertEndpointMatch("GET", "/test/petstore/cats/nope", 404, api1);
      assertEndpointMatch("GET", "/test/petstore/cats/nope/none", 404, api1);

      //the Endpoint constructor will strip /* from gamestop/* because it conflicts with having passed in includePaths
      assertEndpointMatch("GET", "/test/gamestop/nintendo", 200, "ep7", "gamestop", "nintendo", null, null, api1);
      assertEndpointMatch("GET", "/test/gamestop/nintendo/game", 404, api1);
      assertEndpointMatch("GET", "/test/gamestop/xbox/somegame", 200, "ep7", "gamestop", "xbox", "somegame", null, api1);
      assertEndpointMatch("GET", "/test/gamestop/nintendo/sega", 404, api1);

   }

   @Test
   public void test1()
   {
      Engine engine = null;

      engine = new Engine()//
                           .withApi(new Api()//
                                             .withEndpoint("get", "/*")//
                                             .withAction(new MockActionA())//
                                             .withDb(new MockDb()));

      Response resp = engine.get("users");
      resp.dump();
      assertEquals(200, resp.getStatusCode());
      assertEquals("tester1", resp.find("data.0.firstName"));

      //action is placed on the endpoint instead of the api
      engine = new Engine()//
                           .withApi(new Api()//
                                             .withEndpoint("get", "/*", new MockActionA())//
                                             .withDb(new MockDb()));

      resp = engine.get("users");
      assertEquals("tester1", resp.find("data.0.firstName"));

      engine = new Engine()//
                           .withApi(new Api("testApi")//
                                                      .withEndpoint("get", "*", new MockActionA())//
                                                      .withDb(new MockDb()));

      resp = engine.get("users");
      assertEquals(400, resp.getStatusCode());

      resp = engine.get("testApi/users");
      assertEquals(200, resp.getStatusCode());
      assertEquals("tester1", resp.find("data.0.firstName"));

      assertEquals(200, engine.get("/testApi/users").getStatusCode());
      assertEquals(200, engine.get("http://localhost/testApi/users").getStatusCode());
      assertEquals(200, engine.get("http://whateverhost:12345/testApi/users").getStatusCode());
   }

   @Test
   public void test2()
   {
      Engine engine = null;

      engine = new Engine()//
                           .withApi(new Api()//
                                             .withEndpoint("get", "actionA/*", new MockActionA("get", "*"))//
                                             .withEndpoint("get", "actionB/*", new MockActionB("get", "*")));

      Response resp = null;
      JSNode data = null;

      resp = engine.get("/actionA/helloworld");
      data = resp.getJson();
      assertEquals("MockActionA", data.find("data.0.className"));

      resp = engine.get("/actionB/hellomoon");
      data = resp.getJson();
      assertEquals("MockActionB", data.find("data.0.className"));

   }

   @Test
   public void testSlashCorrection()
   {
      assertEquals("a/b", new Path("/a////b/////").toString());
   }

   @Test
   public void testSimpleEndpoint2()
   {
      Engine engine = null;

      engine = new Engine()//
                           .withApi(new Api()//
                                             .withEndpoint("GET", "actionA/*", new MockActionA("GET", "*")));

      Response resp = null;
      resp = engine.get("/actionA");
      assertEquals(200, resp.getStatusCode());

      resp = engine.get("actionA");
      assertEquals(200, resp.getStatusCode());

   }

   /**
    * ...are configed params set to query?  How
    */
   @Test
   public void testConfigParamOverrides()
   {
      //includes/excludes, expands, requires/restricts
      Endpoint ep = new Endpoint("GET", "/", "*").withConfig("endpointParam=endpointValue&overriddenParam=endpointValue");
      Action actionA = new MockAction()
         {
            public void run(io.inversion.cloud.model.Request req, Response res) throws Exception
            {
               Chain.debug("Endpoint_actionA_overriddenParam " + req.getChain().getConfig("overriddenParam"));
               Chain.debug("Endpoint_actionA_endpointParam " + req.getChain().getConfig("endpointParam"));
               Chain.debug("Endpoint_actionA_actionAParam " + req.getChain().getConfig("actionAParam"));
               Chain.debug("Endpoint_actionA_actionBParam " + req.getChain().getConfig("actionBParam"));
               Chain.debug("Endpoint_actionA_actionCParam " + req.getChain().getConfig("actionCParam"));
            }
         }.withConfig("overriddenParam=actionAOverride&actionAParam=actionAValue");

      Action actionB = new MockAction()
         {
            public void run(io.inversion.cloud.model.Request req, Response res) throws Exception
            {
               Chain.debug("Endpoint_actionB_overriddenParam " + req.getChain().getConfig("overriddenParam"));
               Chain.debug("Endpoint_actionB_endpointParam " + req.getChain().getConfig("endpointParam"));
               Chain.debug("Endpoint_actionB_actionAParam " + req.getChain().getConfig("actionAParam"));
               Chain.debug("Endpoint_actionB_actionBParam " + req.getChain().getConfig("actionBParam"));
               Chain.debug("Endpoint_actionB_actionCParam " + req.getChain().getConfig("actionCParam"));

            }
         }.withConfig("overriddenParam=actionBOverride&actionBParam=actionBValue");

      Action actionC = new MockAction()
         {
            public void run(io.inversion.cloud.model.Request req, Response res) throws Exception
            {
               Chain.debug("Endpoint_actionC_overriddenParam " + req.getChain().getConfig("overriddenParam"));
               Chain.debug("Endpoint_actionC_endpointParam " + req.getChain().getConfig("endpointParam"));
               Chain.debug("Endpoint_actionC_actionAParam " + req.getChain().getConfig("actionAParam"));
               Chain.debug("Endpoint_actionC_actionBParam " + req.getChain().getConfig("actionBParam"));
               Chain.debug("Endpoint_actionC_actionCParam " + req.getChain().getConfig("actionCParam"));

            }
         }.withConfig("overriddenParam=actionCOverride&actionCParam=actionCValue");

      ep.withAction(actionA);
      ep.withAction(actionB);
      ep.withAction(actionC);

      Engine engine = new Engine()//
                                  .withApi(new Api("test")//
                                                          .withEndpoint(ep));

      Response res = engine.get("test/test");
      res.dump();

      res.assertDebug("Endpoint_actionA_overriddenParam", "actionAOverride");
      res.assertDebug("Endpoint_actionA_endpointParam", "endpointValue");
      res.assertDebug("Endpoint_actionA_actionAParam", "actionAValue");
      res.assertDebug("Endpoint_actionA_actionBParam", "null");
      res.assertDebug("Endpoint_actionA_actionCParam", "null");

      res.assertDebug("Endpoint_actionB_overriddenParam", "actionBOverride");
      res.assertDebug("Endpoint_actionB_endpointParam", "endpointValue");
      res.assertDebug("Endpoint_actionB_actionAParam", "actionAValue");
      res.assertDebug("Endpoint_actionB_actionBParam", "actionBValue");
      res.assertDebug("Endpoint_actionB_actionCParam", "null");

      res.assertDebug("Endpoint_actionC_overriddenParam", "actionCOverride");
      res.assertDebug("Endpoint_actionC_endpointParam", "endpointValue");
      res.assertDebug("Endpoint_actionC_actionAParam", "actionAValue");
      res.assertDebug("Endpoint_actionC_actionBParam", "actionBValue");
      res.assertDebug("Endpoint_actionC_actionCParam", "actionCValue");

   }

   @Test
   public void test_api_with_version()
   {
      Api api1 = new Api("test")//
                                .withVersion("v1").withAction(new MockAction("mock1").withIncludePaths("*"))//
                                .withEndpoint(new Endpoint("GET", "*").withName("ep1").withExcludePaths("subpath/*"))//
                                .withEndpoint(new Endpoint("GET", "subpath/*").withName("ep2"))//
      ;
      Api api2 = new Api("test")//
                                .withVersion("v2").withAction(new MockAction("mock1").withIncludePaths("*"))//
                                .withEndpoint(new Endpoint("GET", "*").withName("ep3").withExcludePaths("subpath/*"))//
                                .withEndpoint(new Endpoint("GET", "subpath/*").withName("ep4"))//
      ;

      Api api3 = new Api("test")//
                                .withVersion("v3").withAction(new MockAction("mock1").withIncludePaths("*"))//
                                .withEndpoint(new Endpoint("GET", "*").withName("ep5").withExcludePaths("subpath/*"))//
                                .withEndpoint(new Endpoint("GET", "subpath/*").withName("ep6"))//
      ;

      assertEndpointMatch("GET", "http://localhost/test/v1/colKey/entKey/relKey", 200, "ep1", "", "colKey", "entKey", "relKey", api1, api2, api3);
      assertEndpointMatch("GET", "http://localhost/test/v1/subpath/colKey/entKey/relKey", 200, "ep2", "subpath", "colKey", "entKey", "relKey", api1, api2, api3);
      assertEndpointMatch("GET", "http://localhost/test/v2/subpath/colKey/entKey/relKey", 200, "ep4", "subpath", "colKey", "entKey", "relKey", api1, api2, api3);
      assertEndpointMatch("GET", "http://localhost/test/v3/subpath/colKey/entKey/relKey", 200, "ep6", "subpath", "colKey", "entKey", "relKey", api1, api2, api3);

   }

   public static void assertEndpointMatch(String method, String url, int statusCode, Api... apis)
   {
      assertEndpointMatch(method, url, statusCode, null, null, null, null, null, apis);
   }

   public static void assertEndpointMatch(String method, String url, int statusCode, String endpointName, String endpointPath, String collectionKey, String entityKey, String subCollectionKey, Api... apis)
   {
      final boolean[] success = new boolean[]{false};
      Engine e = new Engine()
         {
            protected void run(Chain chain, List<Action> actions) throws Exception
            {
               if (endpointName != null && !endpointName.equals(chain.getRequest().getEndpoint().getName()))
                  fail(chain, "endpoints don't match");

               if (endpointPath != null && !endpointPath.equals(chain.getRequest().getEndpointPath().toString()))
               {
                  fail(chain, "endpoint path doesn't match");
               }

               if (collectionKey != null && !collectionKey.equals(chain.getRequest().getCollectionKey()))
                  fail(chain, "collectionKey don't match");

               if (entityKey != null && !entityKey.equals(chain.getRequest().getEntityKey()))
                  fail(chain, "entityKey don't match");

               if (subCollectionKey != null && !subCollectionKey.equals(chain.getRequest().getSubCollectionKey()))
                  fail(chain, "subCollectionKey don't match");

               success[0] = true;
            }

            protected void fail(Chain chain, String message, Object... vals)
            {
               System.err.print(message);
               for (int i = 0; vals != null && i < vals.length; i++)
                  System.err.print(vals[i] + " ");
               System.err.println("");
               System.err.println(endpointName + "," + endpointPath + "," + collectionKey + "," + entityKey + "," + subCollectionKey);
               System.err.println("url              :" + chain.getRequest().getUrl());
               System.err.println("apiUrl           :" + chain.getRequest().getApiUrl());
               System.err.println("endpoint         :" + chain.getRequest().getEndpoint().getName() + " - " + chain.getRequest().getEndpoint());
               System.err.println("ep path          :" + chain.getRequest().getEndpointPath());
               System.err.println("collectionKey    :" + chain.getRequest().getCollectionKey());
               System.err.println("entityKey        :" + chain.getRequest().getEntityKey());
               System.err.println("subCollectionKey :" + chain.getRequest().getSubCollectionKey());
               System.err.println("subPath          :" + chain.getRequest().getSubpath());

               throw new RuntimeException(message);
            }
         };

      for (Api api : apis)
      {
         if (api != null)
            e.withApi(api);//without this additional API, any apiName will match
      }

      Response resp = e.service(method, url);
      resp.dump();

      if (statusCode != resp.getStatusCode())
         fail("status code mismatch");

      if (statusCode < 400 && !success[0])
         fail("status code mismatch");
   }

}
