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
package io.inversion.cloud.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class PathTest
{
   @Test
   public void extract_stopsOnWildcard()
   {
      Map params = new HashMap();

      Path rule = new Path("part1/part2/*");
      Path path = new Path("/part1/part2/part3/part4");

      Path matched = rule.extract(params, path);

      assertEquals(0, params.size());
      assertEquals("part1/part2", matched.toString());
      assertEquals("part3/part4", path.toString());
   }

   @Test
   public void extract_stopsOnOptional()
   {
      Map params = new HashMap();

      Path rule = new Path("part1/[part2]/part3/*");
      Path path = new Path("/part1/part2/part3/part4");

      Path matched = rule.extract(params, path);

      assertEquals(0, params.size());
      assertEquals("part1", matched.toString());
      assertEquals("part2/part3/part4", path.toString());
   }

   @Test
   public void extract_stopsOnOptionalWithColonVarParsing()
   {
      Map<String, String> params = new HashMap();

      Path rule = new Path("part1/:part2/part3/*");
      Path path = new Path("part1/val2/part3/part4");

      Path matched = rule.extract(params, path);

      assertEquals(1, params.size());
      assertEquals("val2", params.get("part2"));
      assertEquals("part1/val2/part3", matched.toString());
      assertEquals("part4", path.toString());
   }

   @Test
   public void extract_stopsOnOptionalWithBracketVarParsing()
   {
      Map<String, String> params = new HashMap();

      Path rule = new Path("part1/{part2}/part3/*");
      Path path = new Path("part1/val2/part3/part4");

      Path matched = rule.extract(params, path);

      assertEquals(1, params.size());
      assertEquals("val2", params.get("part2"));
      assertEquals("part1/val2/part3", matched.toString());
      assertEquals("part4", path.toString());
   }

   @Test
   public void extract_stopsOnOptionalWithRegexVarParsing()
   {
      Map<String, String> params = new HashMap();

      Path rule = new Path("part1/{part2:[0-9a-zA-Z]{1,8}}/part3/*");
      Path path = new Path("part1/val2/part3/part4");

      Path matched = rule.extract(params, path);

      assertEquals(1, params.size());
      assertEquals("val2", params.get("part2"));
      assertEquals("part1/val2/part3", matched.toString());
      assertEquals("part4", path.toString());
   }

   @Test
   public void extract_regexDoesNotMatch_error()
   {
      boolean error = false;
      try
      {
         Map<String, String> params = new HashMap();
         Path rule = new Path("part1/{part2:[0-9a-zA-Z]{1,8}}/part3/*");
         Path path = new Path("part1/23452345234523452345/part3/part4");

         Path matched = rule.extract(params, path);
      }
      catch (Exception e)
      {
         error = true;
      }
      assertTrue(error, "The test should have errored because 'the regex does not match'");
   }
   
   @Test
   public void extract_pathDoesNotMatch_error()
   {
      boolean error = false;
      try
      {
         Map<String, String> params = new HashMap();
         Path rule = new Path("part1/part2/part3/*");
         Path path = new Path("part1/part5/part3/part4");
         Path matched = rule.extract(params, path);
      }
      catch (Exception e)
      {
         error = true;
      }
      assertTrue(error, "The test should have errored because 'the paths do not match'");
   }
   

   @Test
   public void extract_stopsOnOptionalWithDollarVarParsing()
   {
      Map<String, String> params = new HashMap();

      Path rule = new Path("part1/${part2}/part3/*");
      Path path = new Path("part1/val2/part3/part4");

      Path matched = rule.extract(params, path);

      assertEquals(1, params.size());
      assertEquals("val2", params.get("part2"));
      assertEquals("part1/val2/part3", matched.toString());
      assertEquals("part4", path.toString());
   }

   @Test
   public void extract_missingClosingBracketIsConsideredLiteralNotVariable()
   {
      Map<String, String> params = new HashMap();

      Path rule = new Path("part1/{part2/part3/*");
      Path path = new Path("part1/val2/part3/part4");

      boolean error = false;
      try
      {
         rule.extract(params, path);
      }
      catch (Exception ex)
      {
         error = true;
      }

      assertTrue(error, "The test should have errored because '{part2' is not a literal match fror 'val2'");
   }

   @Test
   public void test_pathMatches()
   {
      //      assertTrue(new Path().pathMatches("[{^$}]", ""));
      //
      //      assertTrue(new Path().pathMatches("*", "/something/asdfas/"));
      //      assertTrue(new Path().pathMatches("*", "something/asdfas/"));
      //      assertTrue(new Path().pathMatches("something/{collection:books|customers}", "something/books"));
      //      assertTrue(new Path().pathMatches("something/{collection:books|customers}", "something/Books"));
      //      assertTrue(new Path().pathMatches("something/{collection:books|customers}", "something/customers"));
      //      assertFalse(new Path().pathMatches("something/{collection:books|customers}", "something/blah"));
      //      assertTrue(new Path().pathMatches("something/{collection:books|customers}/*", "something/customers/1234"));
      //
      //      assertTrue(new Path().pathMatches("something/{collection:books|customers}/{entity:[0-9a-fA-F]{1,8}}", "something/customers/11111111"));
      //      assertTrue(new Path().pathMatches("something/{collection:books|customers}/{entity:[0-9a-fA-F]{1,8}}", "something/customers/aaaaaaaa"));
      //      assertFalse(new Path().pathMatches("something/{collection:books|customers}/{entity:[0-9a-fA-F]{1,8}}", "something/customers/aaaaaaaaaa"));
      //      assertFalse(new Path().pathMatches("something/{collection:books|customers}/{entity:[0-9a-fA-F]{1,8}}", "something/customers/1111111111"));
      //      assertFalse(new Path().pathMatches("something/{collection:books|customers}/{entity:[0-9a-fA-F]{1,8}}", "something/customers/zzzzzzzz"));
      //
      //      assertTrue(new Path().pathMatches("something/{collection:books|customers}/{entity:[0-9]{1,8}}/{relationship:[a-zA-Z]*}", "something/customers/1234/orders"));
      //      assertTrue(new Path().pathMatches("something/{collection:books|customers}/{entity:[0-9]{1,8}}/{relationship:[a-zA-Z]*}", "something/customers/1234/orders/"));
      //      assertTrue(new Path().pathMatches("something/{collection:books|customers}/[{entity:[0-9]{1,8}}]/[{relationship:[a-zA-Z]*}]", "something/customers/1234/"));
      //      assertFalse(new Path().pathMatches("something/{collection:books|customers}/{entity:[0-9]{1,8}}/{relationship:[a-zA-Z]*}", "something/customers/1234/"));
      //
      //      assertTrue(new Path().pathMatches("{collection:players|locations|ads}/[{entity:[0-9]{1,12}}]/{relationship:[a-z]*}", "Locations/698/players"));
   }
}
