/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
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

import java.io.StringWriter;
import java.util.Formatter;
import java.util.Locale;

import io.inversion.cloud.utils.Utils;

public class ApiException extends RuntimeException implements Status
{
   protected String status = Status.SC_500_INTERNAL_SERVER_ERROR;

   private ApiException(String status)
   {
      super(status);
      if (status.matches("\\d\\d\\d .*"))
         withStatus(status);
   }

   private ApiException(String status, String message)
   {
      super(message);
      withStatus(status);
   }

   private ApiException(String status, String message, Throwable t)
   {
      super(message, t);
      withStatus(status);
   }

   public String getStatus()
   {
      return status;
   }

   public ApiException withStatus(String status)
   {
      this.status = status;
      return this;
   }

   public boolean hasStatus(int... statusCodes)
   {
      for (int statusCode : statusCodes)
      {
         if (status.startsWith(statusCode + " "))
            return true;
      }
      return false;
   }

   public static void throw400BadRequest() throws ApiException
   {
      throwEx(SC_400_BAD_REQUEST, null, null);
   }

   public static void throw400BadRequest(Throwable cause) throws ApiException
   {
      throwEx(SC_400_BAD_REQUEST, cause, null);
   }

   public static void throw400BadRequest(String messageFormat, Object... messages) throws ApiException
   {
      throwEx(SC_400_BAD_REQUEST, null, messageFormat, messages);
   }

   public static void throw400BadRequest(Throwable cause, String messageFormat, Object... messages) throws ApiException
   {
      throwEx(SC_400_BAD_REQUEST, cause, messageFormat, messages);
   }

   public static void throw401Unauthroized() throws ApiException
   {
      throwEx(SC_401_UNAUTHORIZED, null, null);
   }

   public static void throw401Unauthroized(Throwable cause) throws ApiException
   {
      throwEx(SC_401_UNAUTHORIZED, cause, null);
   }

   public static void throw401Unauthroized(String messageFormat, Object... messages) throws ApiException
   {
      throwEx(SC_401_UNAUTHORIZED, null, messageFormat, messages);
   }

   public static void throw401Unauthroized(Throwable cause, String messageFormat, Object... messages) throws ApiException
   {
      throwEx(SC_401_UNAUTHORIZED, cause, messageFormat, messages);
   }

   public static void throw403Forbidden() throws ApiException
   {
      throwEx(SC_403_FORBIDDEN, null, null);
   }

   public static void throw403Forbidden(Throwable cause) throws ApiException
   {
      throwEx(SC_403_FORBIDDEN, cause, null);
   }

   public static void throw403Forbidden(String messageFormat, Object... messages) throws ApiException
   {
      throwEx(SC_403_FORBIDDEN, null, messageFormat, messages);
   }

   public static void throw403Forbidden(Throwable cause, String messageFormat, Object... messages) throws ApiException
   {
      throwEx(SC_403_FORBIDDEN, cause, messageFormat, messages);
   }

   public static void throw404NotFound() throws ApiException
   {
      throwEx(SC_404_NOT_FOUND, null, null);
   }

   public static void throw404NotFound(Throwable cause) throws ApiException
   {
      throwEx(SC_404_NOT_FOUND, cause, null);
   }

   public static void throw404NotFound(String messageFormat, Object... messages) throws ApiException
   {
      throwEx(SC_404_NOT_FOUND, null, messageFormat, messages);
   }

   public static void throw404NotFound(Throwable cause, String messageFormat, Object... messages) throws ApiException
   {
      throwEx(SC_404_NOT_FOUND, cause, messageFormat, messages);
   }

   public static void throw429TooManyRequests() throws ApiException
   {
      throwEx(SC_429_TOO_MANY_REQUESTS, null, null);
   }

   public static void throw429TooManyRequests(Throwable cause) throws ApiException
   {
      throwEx(SC_429_TOO_MANY_REQUESTS, cause, null);
   }

   public static void throw429TooManyRequests(String messageFormat, Object... messages) throws ApiException
   {
      throwEx(SC_429_TOO_MANY_REQUESTS, null, messageFormat, messages);
   }

   public static void throw429TooManyRequests(Throwable cause, String messageFormat, Object... messages) throws ApiException
   {
      throwEx(SC_429_TOO_MANY_REQUESTS, cause, messageFormat, messages);
   }

   public static void throw500InternalServerError() throws ApiException
   {
      throwEx(SC_500_INTERNAL_SERVER_ERROR, null, null);
   }

   public static void throw500InternalServerError(Throwable cause) throws ApiException
   {
      throwEx(SC_500_INTERNAL_SERVER_ERROR, cause, null);
   }

   public static void throw500InternalServerError(String messageFormat, Object... messages) throws ApiException
   {
      throwEx(SC_500_INTERNAL_SERVER_ERROR, null, messageFormat, messages);
   }

   public static void throw500InternalServerError(Throwable cause, String messageFormat, Object... messages) throws ApiException
   {
      throwEx(SC_500_INTERNAL_SERVER_ERROR, cause, messageFormat, messages);
   }

   public static void throw501NotImplemented() throws ApiException
   {
      throwEx(SC_501_NOT_IMPLEMENTED, null, null);
   }

   public static void throw501NotImplemented(Throwable cause) throws ApiException
   {
      throwEx(SC_501_NOT_IMPLEMENTED, cause, null);
   }

   public static void throw501NotImplemented(String messageFormat, Object... messages) throws ApiException
   {
      throwEx(SC_501_NOT_IMPLEMENTED, null, messageFormat, messages);
   }

   public static void throw501NotImplemented(Throwable cause, String messageFormat, Object... messages) throws ApiException
   {
      throwEx(SC_501_NOT_IMPLEMENTED, cause, messageFormat, messages);
   }

   /**
    * Rethrows <code>cause</code> as a 500  INTERNAL SERVER ERROR ApiException
    * 
    * @param cause
    * @throws ApiException
    */
   public static void throwEx(Throwable cause) throws ApiException
   {
      throwEx(SC_500_INTERNAL_SERVER_ERROR, cause, null);
   }

   /**
    * Throws a 500 INTERNAL SERVER ERROR ApiException with the given message
    * 
    * @param messageFormat
    * @param messages
    * @throws ApiException
    */
   public static void throwEx(String messageFormat, Object... messages) throws ApiException
   {
      throwEx(SC_500_INTERNAL_SERVER_ERROR, null, messageFormat, messages);
   }

   public static void throwEx(String status, Throwable cause, String messageFormat, Object... args) throws ApiException
   {
      cause = cause != null ? Utils.getCause(cause) : cause;

      String msg = "";
      if (messageFormat == null && cause != null)
      {
         msg = Utils.getShortCause(cause);
      }
      else
      {
         if (messageFormat != null)
         {
            if (args != null && args.length > 0)
            {
               StringWriter sw = new StringWriter();
               Formatter fmt = new Formatter(sw);
               fmt.format(Locale.getDefault(), messageFormat, args);
               fmt.close();
               msg = sw.toString();
            }
            else
               msg = messageFormat;
         }

      }

      throw new ApiException(status, msg, cause);
   }

}
