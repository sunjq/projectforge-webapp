/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2011 Kai Reinhard (k.reinhard@me.com)
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

package org.projectforge.user;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.projectforge.common.ReflectionToString;
import org.projectforge.core.AbstractBaseDO;
import org.projectforge.core.BaseDO;
import org.projectforge.core.Configuration;
import org.projectforge.core.DefaultBaseDO;
import org.projectforge.core.ShortDisplayNameCapable;

/**
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@Entity
@Indexed
@Table(name = "T_PF_USER", uniqueConstraints = { @UniqueConstraint(columnNames = { "username"})})
public class PFUserDO extends DefaultBaseDO implements ShortDisplayNameCapable
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(PFUserDO.class);

  private static final long serialVersionUID = 6680346054753032534L;

  static {
    invalidHistorizableProperties.add("loginFailures");
    invalidHistorizableProperties.add("lastLogin");
    invalidHistorizableProperties.add("stayLoggedInKey");
  }

  private transient Map<String, Object> attributeMap;

  @Field(index = Index.TOKENIZED, store = Store.NO)
  private String username;

  @Field(index = Index.TOKENIZED, store = Store.NO)
  private String jiraUsername;

  private String password;

  @Field(index = Index.TOKENIZED, store = Store.NO)
  private String firstname;

  @Field(index = Index.TOKENIZED, store = Store.NO)
  private String lastname;

  @Field(index = Index.TOKENIZED, store = Store.NO)
  private String description;

  @Field(index = Index.TOKENIZED, store = Store.NO)
  private String email;

  private String stayLoggedInKey;

  private Timestamp lastLogin;

  private int loginFailures;

  private Locale locale;

  private TimeZone timeZone;

  private Locale clientLocale;

  @org.hibernate.search.annotations.Field(index = Index.TOKENIZED, store = Store.NO)
  private String organization;

  @org.hibernate.search.annotations.Field(index = Index.TOKENIZED, store = Store.NO)
  private String personalPhoneIdentifiers;

  @org.hibernate.search.annotations.Field(index = Index.TOKENIZED, store = Store.NO)
  private String personalMebMobileNumbers;

  private Set<UserRightDO> rights = new HashSet<UserRightDO>();

  @Transient
  public String getShortDisplayName()
  {
    return getUsername();
  }

  @Column(length = 255)
  public String getOrganization()
  {
    return organization;
  }

  public void setOrganization(String organization)
  {
    this.organization = organization;
  }

  /**
   * @return For example "Europe/Berlin" if time zone is given otherwise empty string.
   */
  @Transient
  public String getTimeZoneDisplayName()
  {
    if (timeZone == null) {
      return "";
    }
    return timeZone.getDisplayName();
  }

  /**
   * @return For example "Europe/Berlin" if time zone is given otherwise empty string.
   */
  @Column(name = "time_zone")
  public String getTimeZone()
  {
    if (timeZone == null) {
      return "";
    }
    return timeZone.getID();
  }

  public void setTimeZone(String timeZoneId)
  {
    if (StringUtils.isNotBlank(timeZoneId) == true) {
      setTimeZone(TimeZone.getTimeZone(timeZoneId));
    }
  }

  public void setTimeZone(TimeZone timeZone)
  {
    this.timeZone = timeZone;
  }

  @Transient
  public TimeZone getTimeZoneObject()
  {
    if (timeZone != null) {
      return this.timeZone;
    } else {
      return Configuration.getInstance().getDefaultTimeZone();
    }
  }

  public void setTimeZoneObject(final TimeZone timeZone)
  {
    this.timeZone = timeZone;
  }

  @Column
  public Locale getLocale()
  {
    return locale;
  }

  public void setLocale(Locale locale)
  {
    this.locale = locale;
  }

  /**
   * Eine kommaseparierte Liste mit den Kennungen des/der Telefon(e) des Mitarbeiters an der unterstützten Telefonanlage, &zB; zur
   * Direktwahl aus ProjectForge heraus.
   */
  @Column(name = "personal_phone_identifiers", length = 255)
  public String getPersonalPhoneIdentifiers()
  {
    return personalPhoneIdentifiers;
  }

  public void setPersonalPhoneIdentifiers(String personalPhoneIdentifiers)
  {
    this.personalPhoneIdentifiers = personalPhoneIdentifiers;
  }

  /**
   * A comma separated list of all personal mobile numbers from which SMS can be send. Those SMS will be assigned to this user. <br/>
   * This is a feature from the Mobile Enterprise Blogging.
   */
  @Column(name = "personal_meb_identifiers", length = 255)
  public String getPersonalMebMobileNumbers()
  {
    return personalMebMobileNumbers;
  }

  public void setPersonalMebMobileNumbers(String personalMebMobileNumbers)
  {
    this.personalMebMobileNumbers = personalMebMobileNumbers;
  }

  /**
   * Returns string containing all fields (except the password) of given user object (via ReflectionToStringBuilder).
   * @param user
   * @return
   */
  public String toString()
  {
    return (new ReflectionToString(this) {
      protected boolean accept(java.lang.reflect.Field f)
      {
        return super.accept(f) && !"password".equals(f.getName()) && !"stayLoggedInKey".equals(f.getName());
      }
    }).toString();
  }

  @Transient
  public String getUserDisplayname()
  {
    String str = getFullname();
    if (StringUtils.isNotBlank(str) == true) {
      return str + " (" + getUsername() + ")";
    }
    return getUsername();
  }

  @Override
  public boolean equals(Object o)
  {
    if (o instanceof PFUserDO) {
      PFUserDO other = (PFUserDO) o;
      if (ObjectUtils.equals(this.getUsername(), other.getUsername()) == false)
        return false;
      return true;
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    return getUsername() == null ? 0 : getUsername().hashCode();
  }

  public boolean copyValuesFrom(BaseDO< ? extends Serializable> src, String... ignoreFields)
  {
    ignoreFields = (String[]) ArrayUtils.add(ignoreFields, "password"); // NPE save considering ignoreFields
    PFUserDO user = (PFUserDO) src;
    boolean modified = AbstractBaseDO.copyValues(user, this, ignoreFields);
    if (user.getPassword() != null) {
      setPassword(user.getPassword());
      checkAndFixPassword();
      if (getPassword() != null) {
        modified = true;
      }
    }
    return modified;
  }

  /**
   * If password is not given as "SHA{..." then it will be set to null due to security reasons.
   */
  protected void checkAndFixPassword()
  {
    if (StringUtils.isNotEmpty(getPassword()) && getPassword().startsWith("SHA{") == false) {
      setPassword(null);
      log.error("Password for user '" + getUsername() + "' is not given SHA encrypted. Ignoring it.");
    }
  }

  @SuppressWarnings("unchecked")
  @Transient
  public Set getHistorizableAttributes()
  {
    return null;
  }

  /**
   * The locale given from the client (e. g. from the browser by the http request). This locale is needed by PFUserContext for getting the
   * browser locale if the user's locale is null and the request's locale is not available.
   * @return
   */
  @Transient
  public Locale getClientLocale()
  {
    return clientLocale;
  }

  public PFUserDO setClientLocale(Locale clientLocale)
  {
    this.clientLocale = clientLocale;
    return this;
  }

  /**
   * Do nothing.
   * @see org.projectforge.core.ExtendedBaseDO#recalculate()
   */
  public void recalculate()
  {
  }

  public Object getAttribute(String key)
  {
    if (attributeMap == null) {
      return null;
    }
    return attributeMap.get(key);
  }

  public void setAttribute(String key, Object value)
  {
    synchronized (this) {
      if (attributeMap == null) {
        attributeMap = new HashMap<String, Object>();
      }
      attributeMap.put(key, value);
    }
  }

  public void removeAttribute(String key)
  {
    if (attributeMap == null) {
      return;
    }
    attributeMap.remove(key);
  }

  /**
   * @return Returns the username.
   */
  @Column(length = 255, unique = true, nullable = false)
  public String getUsername()
  {
    return username;
  }

  /**
   * @param username The username to set.
   * @return this for chaining.
   */
  public PFUserDO setUsername(final String username)
  {
    this.username = username;
    return this;
  }

  /**
   * Die E-Mail Adresse des Benutzers, falls vorhanden.
   * @return Returns the email.
   */
  @Column(length = 255)
  public String getEmail()
  {
    return email;
  }

  /**
   * @param email The email to set.
   */
  public void setEmail(String email)
  {
    Validate.isTrue(email == null || email.length() <= 255, email);
    this.email = email;
  }

  /**
   * Key stored in the cookies for the functionality of stay logged in.
   */
  @Column(name = "stay_logged_in_key", length = 255)
  public String getStayLoggedInKey()
  {
    return stayLoggedInKey;
  }

  public void setStayLoggedInKey(String stayLoggedInKey)
  {
    this.stayLoggedInKey = stayLoggedInKey;
  }

  /**
   * Der Vorname des Benutzer.
   * @return Returns the firstname.
   */
  @Column(length = 255)
  public String getFirstname()
  {
    return firstname;
  }

  /**
   * @param firstname The firstname to set.
   * @return this for chaining.
   */
  public PFUserDO setFirstname(String firstname)
  {
    Validate.isTrue(firstname == null || firstname.length() <= 255, firstname);
    this.firstname = firstname;
    return this;
  }

  /**
   * Gibt den Vor- und Nachnamen zurück, falls gegeben. Vor- und Nachname sind durch ein Leerzeichen getrennt.
   * @return String
   */
  @Transient
  public String getFullname()
  {
    StringBuffer name = new StringBuffer();
    if (this.firstname != null) {
      name.append(this.firstname);
      name.append(" ");
    }
    if (this.lastname != null) {
      name.append(this.lastname);
    }

    return name.toString();
  }

  /**
   * Zeitstempel des letzten erfolgreichen Logins.
   * @return Returns the lastLogin.
   */
  public Timestamp getLastLogin()
  {
    return lastLogin;
  }

  /**
   * @return Returns the lastname.
   */
  @Column(length = 255)
  public String getLastname()
  {
    return lastname;
  }

  /**
   * @return Returns the description.
   */
  @Column(length = 255)
  public String getDescription()
  {
    return description;
  }

  /**
   * @param description The description to set.
   */
  public void setDescription(String description)
  {
    Validate.isTrue(description == null || description.length() <= 255, description);
    this.description = description;
  }

  /**
   * Die Anzahl der erfolglosen Logins. Dieser Wert wird bei dem nächsten erfolgreichen Login auf 0 zurück gesetzt.
   * @return Returns the loginFailures.
   */
  public int getLoginFailures()
  {
    return loginFailures;
  }

  /**
   * JIRA user name (if differ from the ProjectForge's user name) is used e. g. in MEB for creating new issues.
   */
  @Column(name = "jira_username", length = 100)
  public String getJiraUsername()
  {
    return jiraUsername;
  }

  public void setJiraUsername(String jiraUsername)
  {
    this.jiraUsername = jiraUsername;
  }

  /**
   * @return The JIRA user name or if not given the user name (assuming that the JIRA user name is same as ProjectForge user name).
   */
  @Transient
  public String getJiraUsernameOrUsername()
  {
    if (StringUtils.isNotEmpty(jiraUsername) == true) {
      return this.jiraUsername;
    } else {
      return this.username;
    }
  }

  /**
   * Encoded password of the user (SHA-1).
   * @return Returns the password.
   */
  @Column(length = 50)
  public String getPassword()
  {
    return password;
  }

  /**
   * @param password The password to set.
   */
  public void setPassword(String password)
  {
    this.password = password;
  }

  /**
   * @param lastLogin The lastLogin to set.
   */
  public void setLastLogin(Timestamp lastLogin)
  {
    this.lastLogin = lastLogin;
  }

  /**
   * @param lastname The lastname to set.
   * @return this for chaining.
   */
  public PFUserDO setLastname(String lastname)
  {
    this.lastname = lastname;
    return this;
  }

  /**
   * @param loginFailures The loginFailures to set.
   */
  public void setLoginFailures(int loginFailures)
  {
    this.loginFailures = loginFailures;
  }

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "user")
  public Set<UserRightDO> getRights()
  {
    return this.rights;
  }

  public void setRights(Set<UserRightDO> rights)
  {
    this.rights = rights;
  }

  public PFUserDO addRight(final UserRightDO right)
  {
    if (this.rights == null) {
      setRights(new HashSet<UserRightDO>());
    }
    this.rights.add(right);
    right.setUser(this);
    return this;
  }

  @Transient
  public UserRightDO getRight(final UserRightId rightId)
  {
    if (this.rights == null) {
      return null;
    }
    for (final UserRightDO right : this.rights) {
      if (right.getRightId() == rightId) {
        return right;
      }
    }
    return null;
  }

  @Transient
  public String getDisplayUsername()
  {
    return getShortDisplayName();
  }
}
