package io.rocketpartners.cloud.action.sql;

import java.io.File;
import java.sql.Connection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.utils.Sql;
import io.rocketpartners.cloud.utils.Utils;
import junit.framework.TestCase;

public class TestSqlActions extends TestCase
{
   public static final int     TABLE_ORDERS_ROWS = 830;
   public static final int     DEFAULT_MAX_ROWS  = 10;

   static boolean              initedDb          = false;

   static LinkedList<String>   sqls              = new LinkedList<>();

   static Map<String, Service> services          = new HashMap();

   public static synchronized Service service(String apiName, final String ddl)
   {
      Service service = services.get(apiName.toLowerCase());
      if (service != null)
         return service;

      final SqlDb db = new SqlDb();
      db.withDriver("org.h2.Driver");
      db.withUrl("jdbc:h2:./" + ddl + "-" + Utils.time());
      db.withUser("sa");
      db.withPass("");

      service = new Service()
         {
            public void init()
            {
               try
               {
                  File dir = new File("./.h2");
                  dir.mkdir();

                  File[] dbfiles = dir.listFiles();
                  for (int i = 0; dbfiles != null && i < dbfiles.length; i++)
                  {
                     if (dbfiles[i].getName().startsWith(ddl))
                        dbfiles[i].delete();
                  }

                  Connection conn = db.getConnection();
                  Sql.runDdl(conn, TestSqlActions.class.getResourceAsStream(ddl + ".ddl"));

                  //                  Sql.addSqlListener(new SqlListener()
                  //                     {
                  //                        @Override
                  //                        public void beforeStmt(String method, String sql, Object... vals)
                  //                        {
                  //                           sqls.add(method);
                  //                           while (sqls.size() > 0)
                  //                              sqls.removeLast();
                  //                        }
                  //                     });

                  super.init();
               }
               catch (Exception ex)
               {
                  ex.printStackTrace();
                  Utils.rethrow(ex);
               }
            }
         };

      //
      service.withApi(apiName)//
             .withEndpoint("GET", "sql", "*").withAction(new SqlGetAction())//
             .withMaxRows(DEFAULT_MAX_ROWS).getApi()//
             .withDb(new SqlDb()).withConfig("org.h2.Driver", "jdbc:h2:./northwind", "sa", "").getApi().getService();

      services.put(apiName, service);

      return service;
   }

   public static String getSql(int idx)
   {
      return sqls.get(idx);
   }

   public static List<String> getSqls()
   {
      return sqls;
   }

   public static void clearSql()
   {
      sqls.clear();
   }

   /**
    * IMPORTANT - if you are running test cases in springboot/tomcat 
    * this must run AFTER tomcat is bootstrapped.
    * 
    * Seems to be a tomcat side effect dependency on being about 
    * to set java.net.URL.setURLStreamHandlerFactory
    * 
    * @throws Exception
    */
   public static synchronized void initDb(String ddl)
   {
      try
      {

      }
      catch (Exception ex)
      {
         ex.printStackTrace();
         Utils.rethrow(ex);
      }
   }

}