/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
 * http://rocketpartners.io
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package io.rocketpartners.cloud.model;

import java.util.ArrayList;
import java.util.List;

import io.rocketpartners.cloud.utils.Utils;

public class AclRule extends Rule<AclRule>
{
   protected boolean      allow       = true;
   protected boolean      info        = false;

   protected List<String> permissions = new ArrayList();
   protected List<Role>   roles       = new ArrayList();

   protected List<String> restricts   = new ArrayList();
   protected List<String> requires    = new ArrayList();

   public AclRule()
   {
      super();
   }

   public AclRule(String... methods)
   {
      super();
      withMethods(methods);
   }

   public boolean ruleMatches(Request req)
   {
      if (!matches(req.getMethod(), req.getPath()))
         return false;

      //short cut 
      if (req.getUser() == null && (roles.size() > 0 || permissions.size() > 0))
         return false;

      boolean hasRole = false;
      boolean hasPerms = false;

      if (roles.size() == 0)
      {
         hasRole = true;
      }
      else
      {
         for (Role requiredRole : getRoles())
         {
            for (Role userRole : req.getUser().getRoles())
            {
               if (userRole.getLevel() >= requiredRole.getLevel())
               {
                  hasRole = true;
                  break;
               }
            }
            if (hasRole)
               break;
         }
      }

      if (permissions.size() == 0)
      {
         hasPerms = true;
      }
      else
      {
         List rest = new ArrayList(permissions);
         rest.removeAll(req.getUser().getPermissions());
         hasPerms = (rest.size() == 0);
      }

      return hasRole && hasPerms;
   }

   @Override
   public AclRule withApi(Api api)
   {
      if (this.api != api)
      {
         this.api = api;
         api.withAclRule(this);
      }
      return this;
   }

   public ArrayList<Role> getRoles()
   {
      return new ArrayList(roles);
   }

   public AclRule withRoles(List<Role> roles)
   {
      for (Role role : roles)
         withRole(role);

      return this;
   }

   public AclRule withRole(Role role)
   {
      if (!roles.contains(role))
         roles.add(role);

      return this;
   }

   public ArrayList<String> getPermissions()
   {
      return new ArrayList(permissions);
   }

   public AclRule withPermission(String... permissions)
   {
      if (permissions != null)
      {
         for (String permission : Utils.explode(",", permissions))
         {
            if (!this.permissions.contains(permission))
               this.permissions.add(permission);
         }
      }
      return this;
   }

   public AclRule withRestricts(String... restricts)
   {
      if (restricts != null)
      {
         for (String restrict : Utils.explode(",", restricts))
         {
            if (!this.restricts.contains(restrict))
               this.restricts.add(restrict);
         }
      }
      return this;
   }

   public List<String> getRestricts()
   {
      return new ArrayList(restricts);
   }

   public AclRule withRequires(String... requires)
   {
      if (requires != null)
      {
         for (String require : Utils.explode(",", requires))
         {
            if (!this.requires.contains(require))
               this.requires.add(require);
         }
      }
      return this;
   }

   public List<String> getRequires()
   {
      return new ArrayList(requires);
   }

   public boolean isAllow()
   {
      return allow;
   }

   public AclRule withAllow(boolean allow)
   {
      this.allow = allow;
      return this;
   }

   public boolean isInfo()
   {
      return info;
   }

   public AclRule withInfo(boolean info)
   {
      this.info = info;
      return this;
   }

   @Override
   public String toString()
   {
      if (name != null)
      {
         return super.toString();
      }

      return System.identityHashCode(this) + " - " + permissions + " - " + includePaths + " - " + methods;
   }

}
