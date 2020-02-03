/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion.cloud.action.elastic.v03x.rql;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.inversion.cloud.utils.Utils;

public class Parts
{
   public String select = "";
   public String from   = "";
   public String where  = "";
   public String group  = "";
   public String order  = "";
   public String limit  = "";

   public Parts(String sql)
   {
      select = chopFirst(sql, "select", "from");

      if (Utils.empty(select))
         select = chopFirst(sql, "update", "where");

      if (Utils.empty(select))
         select = chopFirst(sql, "delete", "from");

      if (select != null)
      {
         sql = sql.substring(select.length(), sql.length());

         if (sql.trim().substring(4).trim().startsWith("("))
         {
            int end = sql.lastIndexOf("as") + 3;
            String rest = sql.substring(end, sql.length());
            int[] otherIdx = findFirstOfFirstOccurances(rest, "where", " group by", "order", "limit");
            if (otherIdx != null)
            {
               end += otherIdx[0];
            }
            else
            {
               end = sql.length();
            }

            from = sql.substring(0, end);
            sql = sql.substring(end, sql.length());
         }
         else
         {
            from = chopLast(sql, "from", "where", "group by", "order", "limit");
         }
         where = chopLast(sql, "where", "group by", "order", "limit");
         group = chopLast(sql, "group by", "order", "limit");
         order = chopLast(sql, "order", "limit");
         limit = chopLast(sql, "limit");
      }
   }

   public static void main(String[] args)
   {
      int[] found = findFirstOfFirstOccurances("asdasdfa order \r\n \t by asdasdf", "order by ");
      System.out.println(found);
   }

   static int[] findFirstOfFirstOccurances(String haystack, String... regexes)
   {
      int[] first = null;
      for (String regex : regexes)
      {
         regex = "\\b(" + regex.trim().replaceAll(" ", "\\\\s*") + ")\\b";
         Matcher m = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(haystack);
         if (m.find())
         {
            int start = m.start(1);
            int end = m.end(1);

            if (first == null || start < first[0])
               first = new int[]{start, end};
         }
      }
      //      if (first != null)
      //         System.out.println("found first '" + haystack.substring(first[0], first[1]) + "'");
      return first;
   }

   static int[] findFirstOfLastOccurances(String haystack, String... regexes)
   {
      int[] first = null;

      for (String regex : regexes)
      {
         int[] last = null;

         regex = "\\b(" + regex.trim().replaceAll(" ", "\\\\s*") + ")\\b";
         Matcher m = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(haystack);
         while (m.find())
         {
            int start = m.start(1);
            int end = m.end(1);

            if (last == null || start > last[0])
               last = new int[]{start, end};
         }

         if (last != null && (first == null || last[0] < first[0]))
            first = last;
      }
      //      if (first != null)
      //         System.out.println("found first last'" + haystack.substring(first[0], first[1]) + "'");

      return first;
   }

   protected String chopLast(String haystack, String start, String... ends)
   {
      int[] startIdx = findFirstOfFirstOccurances(haystack, start);

      if (startIdx != null)
      {
         int[] endIdx = findFirstOfLastOccurances(haystack, ends);
         if (endIdx != null)
            return haystack.substring(startIdx[0], endIdx[0]).trim() + " ";
         else
            return haystack.substring(startIdx[0], haystack.length()).trim() + " ";
      }
      return null;
   }

   protected String chopFirst(String haystack, String start, String... ends)
   {
      int[] startIdx = findFirstOfFirstOccurances(haystack, start);

      if (startIdx != null)
      {
         int[] endIdx = findFirstOfFirstOccurances(haystack, ends);
         if (endIdx != null)
            return haystack.substring(startIdx[0], endIdx[0]).trim() + " ";
         else
            return haystack.substring(startIdx[0], haystack.length()).trim() + " ";
      }
      return null;
   }

}