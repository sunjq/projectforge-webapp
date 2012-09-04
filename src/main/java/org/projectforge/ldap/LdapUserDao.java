/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2012 Kai Reinhard (k.reinhard@micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.ldap;

import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.commons.lang.StringUtils;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class LdapUserDao extends LdapPersonDao
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(LdapUserDao.class);

  public static final String DEACTIVATED_SUB_CONTEXT = "deactivated";

  private static final String DEACTIVATED_SUB_CONTEXT2 = "ou=" + DEACTIVATED_SUB_CONTEXT;

  private static final String DEACTIVATED_SUB_CONTEXT3 = DEACTIVATED_SUB_CONTEXT2 + ",";

  public static boolean isDeactivated(final LdapPerson user)
  {
    return user.isDeleted() || user.getOrganizationalUnit() != null && LdapUtils.getOu(user.getOrganizationalUnit()).contains(DEACTIVATED_SUB_CONTEXT) == true;
  }

  /**
   * @see org.projectforge.ldap.LdapPersonDao#getIdAttrId()
   */
  @Override
  public String getIdAttrId()
  {
    return "employeeNumber";
  }

  /**
   * @see org.projectforge.ldap.LdapPersonDao#getId(org.projectforge.ldap.LdapPerson)
   */
  @Override
  public String getId(final LdapPerson obj)
  {
    return obj.getEmployeeNumber();
  }

  public void deactivateUser(final LdapPerson user)
  {
    log.info("Deactivate user: " + buildDn(user));
    final ModificationItem[] modificationItems;
    short i = 0;
    modificationItems = new ModificationItem[2];
    modificationItems[i++] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("userPassword", null));
    modificationItems[i++] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("mail", "deactivated@devnull.com"));
    buildDn(user);
    modify(user, modificationItems);
    // Move user to the sub-context "deactivated".
    move(user, LdapUtils.getOu(DEACTIVATED_SUB_CONTEXT, user.getOrganizationalUnit()));
  }

  /**
   * Moves the user only from the "deactivated" sub-context to the parent context. If the user isn't in the context name "deactivated"
   * nothing will be done.
   * @param user
   */
  public void reactivateUser(final LdapPerson user)
  {
    log.info("Reactivate deactivated user: " + buildDn(user));
    final String ou = LdapUtils.getOu(user.getOrganizationalUnit());
    if (ou.startsWith(DEACTIVATED_SUB_CONTEXT2) == false) {
      log.info("Object isn't in a deactivated sub-context, nothing will be done: " + buildDn(user));
      return;
    }
    String newPath;
    if (ou.startsWith(DEACTIVATED_SUB_CONTEXT3) == true) {
      newPath = ou.substring(DEACTIVATED_SUB_CONTEXT3.length());
    } else {
      newPath = ou.substring(DEACTIVATED_SUB_CONTEXT2.length());
    }
    move(user, newPath);
  }

  /**
   * @see org.projectforge.ldap.LdapDao#create(org.projectforge.ldap.LdapObject, java.lang.Object[])
   */
  @Override
  public void create(final LdapPerson obj, final Object... args)
  {
    super.create(obj, args);
    if (obj.isDeleted() == true) {
      deactivateUser(obj);
    }
  }

  public void changePassword(final LdapPerson user, final String oldPassword, final String newPassword)
  {
    log.info("Change password for " + getObjectClass() + ": " + buildDn(user));
    final ModificationItem[] modificationItems;
    // Replace the "unicdodePwd" attribute with a new value
    // Password must be both Unicode and a quoted string
    // try {
    // final String oldQuotedPassword = "\"" + oldPassword + "\"";
    // final byte[] oldUnicodePassword = oldQuotedPassword.getBytes("UTF-16LE");
    // final String newQuotedPassword = "\"" + newPassword + "\"";
    // final byte[] newUnicodePassword = newQuotedPassword.getBytes("UTF-16LE");
    if (oldPassword != null) {
      modificationItems = new ModificationItem[2];
      modificationItems[0] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute("userPassword", oldPassword));
      modificationItems[1] = new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute("userPassword", newPassword));
    } else {
      modificationItems = new ModificationItem[1];
      modificationItems[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("userPassword", newPassword));
    }
    // } catch (final UnsupportedEncodingException ex) {
    // log.error("While encoding passwords with UTF-16LE: " + ex.getMessage(), ex);
    // throw new RuntimeException(ex);
    // }
    // Perform the update
    modify(user, modificationItems);
  }

  public LdapPerson findByUsername(final Object username, final String... organizationalUnits)
  {
    return (LdapPerson) new LdapTemplate(ldapConnector) {
      @Override
      protected Object call() throws NameNotFoundException, Exception
      {
        NamingEnumeration< ? > results = null;
        final SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        final String searchBase = LdapUtils.getOu(organizationalUnits);
        results = ctx.search(searchBase, "(&(objectClass=" + getObjectClass() + ")(uid=" + username + "))", controls);
        if (results.hasMore() == false) {
          return null;
        }
        final SearchResult searchResult = (SearchResult) results.next();
        final String dn = searchResult.getName();
        final Attributes attributes = searchResult.getAttributes();
        if (results.hasMore() == true) {
          log.error("Oups, found entries with multiple id's: " + getObjectClass() + "." + username);
        }
        return mapToObject(dn, searchBase, attributes);
      }
    }.excecute();
  }

  public boolean authenticate(final String username, final String userPassword, final String... organizationalUnits)
  {
    final LdapPerson user = findByUsername(username, organizationalUnits);
    if (user == null || StringUtils.equals(username, user.getId()) == false) {
      log.info("User with id '" + username + "' not found.");
      return false;
    }
    final String dn = user.getDn() + "," + ldapConnector.getBase();
    try {
      ldapConnector.createContext(dn, userPassword);
      log.info("User '" + username + "' (" + dn + ") successfully authenticated.");
      return true;
    } catch (final Exception ex) {
      log.error("User '" + username + "' (" + dn + ") with invalid credentials.");
      return false;
    }
  }
}
