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

package org.projectforge.fibu.kost.reporting;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.ObjectUtils;
import org.projectforge.fibu.KostFormatter;
import org.projectforge.fibu.kost.BuchungssatzDO;
import org.projectforge.fibu.kost.Bwa;
import org.projectforge.fibu.kost.BwaTable;
import org.projectforge.user.PFUserContext;

/**
 * Ein Report enthält unterliegende Buchungssätze, die gemäß Zeitraum und zugehörigem ReportObjective selektiert werden.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class Report implements Serializable
{
  private static final long serialVersionUID = -5359861335173843043L;

  private transient List<BuchungssatzDO> buchungssaetze;

  private transient Set<BuchungssatzDO> buchungssatzSet;

  private transient ReportObjective reportObjective;

  private transient List<Report> childReports;

  private transient List<BuchungssatzDO> other;

  private transient List<BuchungssatzDO> duplicates;

  private boolean showChilds;

  private transient Bwa bwa;

  private transient BwaTable bwaTable;

  private int fromYear;

  private int fromMonth;

  private int toYear;

  private int toMonth;

  private transient Report parent;

  public Report(ReportObjective reportObjective)
  {
    this.reportObjective = reportObjective;
  }

  public Report(ReportObjective reportObjective, Report parent)
  {
    this(reportObjective, parent.fromYear, parent.fromMonth, parent.toYear, parent.toMonth);
    this.parent = parent;
  }

  public Report(ReportObjective reportObjective, int fromYear, int fromMonth, int toYear, int toMonth)
  {
    this(reportObjective);
    this.fromYear = fromYear;
    this.fromMonth = fromMonth;
    this.toYear = toYear;
    this.toMonth = toMonth;
  }

  public void setFrom(int year, int month)
  {
    this.fromYear = year;
    this.fromMonth = month;
  }

  public void setTo(int year, int month)
  {
    this.toYear = year;
    this.toMonth = month;
  }

  public int getFromYear()
  {
    return fromYear;
  }

  public int getFromMonth()
  {
    return fromMonth;
  }

  public int getToYear()
  {
    return toYear;
  }

  public int getToMonth()
  {
    return toMonth;
  }

  public Report getParent()
  {
    return parent;
  }

  /**
   * Gibt den Reportpfad zurück, vom Root-Report bis zum direkten Eltern-Report. Der Report selber ist nicht im Pfad enthalten.
   * @return Liste oder null, wenn der Report keinen Elternreport hat.
   */
  public List<Report> getPath()
  {
    if (this.parent == null) {
      return null;
    }
    List<Report> path = new ArrayList<Report>();
    this.parent.getPath(path);
    return path;
  }

  private void getPath(List<Report> path)
  {
    if (this.parent != null) {
      this.parent.getPath(path);
    }
    path.add(this);
  }

  public ReportObjective getReportObjective()
  {
    return reportObjective;
  }

  public Bwa getBwa()
  {
    if (this.bwa == null) {
      this.bwa = new Bwa();
      this.bwa.setReference(this);
      this.bwa.setStoreBuchungssaetzeInZeilen(true);
      this.bwa.setBuchungssaetze(this.buchungssaetze);
    }
    return this.bwa;
  }

  /**
   * Creates an array with all Bwa's of the child reports.
   * @param prependThisReport If true then the Bwa of this report will be prepend as first column.
   */
  public BwaTable getChildBwaTable(boolean prependThisReport)
  {
    if (bwaTable == null) {
      if (prependThisReport == false && hasChilds() == false) {
        return null;
      }
      bwaTable = new BwaTable();
      if (prependThisReport == true) {
        bwaTable.addBwa(this.getId(), this.getBwa());
      }
      if (hasChilds() == true) {
        for (Report child : getChilds()) {
          bwaTable.addBwa(child.getId(), child.getBwa());
        }
      }
    }
    return bwaTable;
  }

  /**
   * Wurde eine Selektion bereits durchgeführt?
   * @return true, wenn Buchungssätzeliste vorhanden ist (kann aber auf Grund der Selektion auch leer sein).
   */
  public boolean isLoad()
  {
    return this.buchungssaetze != null;
  }

  public boolean isShowChilds()
  {
    return showChilds;
  }

  public void setShowChilds(boolean showChilds)
  {
    this.showChilds = showChilds;
  }

  /**
   * @see ReportObjective#hasChilds()
   */
  public boolean hasChilds()
  {
    return reportObjective.getHasChilds();
  }

  /**
   * @see ReportObjective#getId()
   */
  public String getId()
  {
    return reportObjective.getId();
  }

  /**
   * @see ReportObjective#getTitle()
   */
  public String getTitle()
  {
    return reportObjective.getTitle();
  }

  public Report findById(String id)
  {
    if (ObjectUtils.equals(this.reportObjective.getId(), id) == true) {
      return this;
    }
    if (hasChilds() == false) {
      return null;
    }
    for (Report report : getChilds()) {
      if (ObjectUtils.equals(report.reportObjective.getId(), id) == true) {
        return report;
      }
    }
    for (Report report : getChilds()) {
      Report result = report.findById(id);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  /**
   * Creates and get the childs if the ReportObjective has childs. Iteriert über alle ChildReportObjectives und legt jeweils einen Report an
   * und selektiert gemäß Filter des ReportObjectives die Buchungssätze. Wenn Childs nicht implizit erzeugt werden sollen, so sollte die
   * Funktion hasChilds zur Abfrage genutzt werden.
   * @see #select(List)
   */
  public List<Report> getChilds()
  {
    if (childReports == null && hasChilds() == true) {
      childReports = new ArrayList<Report>();
      for (ReportObjective child : reportObjective.getChildReportObjectives()) {
        Report report = new Report(child, this);
        report.select(this.buchungssaetze);
        childReports.add(report);
      }
      if (this.buchungssaetze != null && (reportObjective.isSuppressOther() == false || reportObjective.isSuppressDuplicates() == false)) {
        for (BuchungssatzDO satz : this.buchungssaetze) {
          int n = 0;
          for (Report child : getChilds()) {
            if (child.contains(satz) == true) {
              n++;
            }
          }
          if (reportObjective.isSuppressOther() == false && n == 0) {
            // Kommt bei keinem Childreport vor:
            if (other == null) {
              other = new ArrayList<BuchungssatzDO>();
            }
            other.add(satz);
          } else if (reportObjective.isSuppressDuplicates() == false && n > 1) {
            // Kommt bei mehreren Childs vor:
            if (duplicates == null) {
              duplicates = new ArrayList<BuchungssatzDO>();
            }
            duplicates.add(satz);
          }
        }
      }
      if (reportObjective.isSuppressOther() == false && this.other != null) {
        ReportObjective objective = new ReportObjective();
        String other = PFUserContext.getLocalizedString("fibu.reporting.other");
        objective.setId(this.getId() + " - " + other);
        objective.setTitle(this.getTitle() + " - " + other);
        Report report = new Report(objective, this);
        report.setBuchungssaetze(this.other);
        childReports.add(report);
      }
      if (reportObjective.isSuppressDuplicates() == false && this.duplicates != null) {
        ReportObjective objective = new ReportObjective();
        String duplicates = PFUserContext.getLocalizedString("fibu.reporting.duplicates");
        objective.setId(this.getId() + " - " + duplicates);
        objective.setTitle(this.getTitle() + " - " + duplicates);
        Report report = new Report(objective, this);
        report.setBuchungssaetze(this.duplicates);
        childReports.add(report);
      }
    }
    return childReports;
  }

  public List<BuchungssatzDO> getBuchungssaetze()
  {
    return buchungssaetze;
  }

  /**
   * Bitte entweder diese Methode ODER select(...) benutzen.
   * @param buchungssaetze
   */
  public void setBuchungssaetze(List<BuchungssatzDO> buchungssaetze)
  {
    this.buchungssaetze = buchungssaetze;
  }

  /**
   * Gibt die Liste aller sonstigen Buchungssätze zurück, d. h. Buchungssätze, die zwar in diesem Report vorkommen aber in keinem der
   * Childreports vorkommen.
   * @return Liste oder null, wenn keine Einträge vorhanden sind.
   */
  public List<BuchungssatzDO> getOther()
  {
    return other;
  }

  /**
   * Gibt die Liste aller doppelten Buchungssätze zurück, d. h. Buchungssätze, die in mehreren Childreports vorkommen.
   * @return Liste oder null, wenn keine Einträge vorhanden sind.
   */
  public List<BuchungssatzDO> getDuplicates()
  {
    return duplicates;
  }

  public String getZeitraum()
  {
    return KostFormatter.formatZeitraum(fromYear, fromMonth, toYear, toMonth);
  }

  /**
   * Diese initiale Liste der Buchungsliste wird sofort bezüglich Exclude- und Include-Filter selektiert und das Ergebnis gesetzt.
   * @param buchungssaetze vor Selektion.
   */
  public void select(List<BuchungssatzDO> list)
  {
    final Predicate regExpPredicate = new Predicate() {
      public boolean evaluate(Object obj)
      {
        final BuchungssatzDO satz = (BuchungssatzDO) obj;
        final String kost1 = KostFormatter.format(satz.getKost1());
        final String kost2 = KostFormatter.format(satz.getKost2());

        // 1st of all the Blacklists
        if (match(reportObjective.getKost1ExcludeRegExpList(), kost1, false) == true) {
          return false;
        }
        if (match(reportObjective.getKost2ExcludeRegExpList(), kost2, false) == true) {
          return false;
        }
        // 2nd the whitelists
        final boolean kost1Match = match(reportObjective.getKost1IncludeRegExpList(), kost1, true);
        final boolean kost2Match = match(reportObjective.getKost2IncludeRegExpList(), kost2, true);
        return kost1Match == true && kost2Match == true;
      }
    };
    this.buchungssaetze = new ArrayList<BuchungssatzDO>();
    this.buchungssatzSet = new HashSet<BuchungssatzDO>();
    this.bwa = null;
    this.bwaTable = null;
    this.childReports = null;
    this.duplicates = null;
    this.other = null;
    CollectionUtils.select(list, regExpPredicate, this.buchungssaetze);
    for (final BuchungssatzDO satz : this.buchungssaetze) {
      this.buchungssatzSet.add(satz);
    }
  }
  
  public boolean contains(BuchungssatzDO satz)
  {
    if (buchungssatzSet == null) {
      return false;
    }
    return this.buchungssatzSet.contains(satz);
  }

  /**
   * In jedem regulärem Ausdruck werden alle Punkte gequoted und alle * durch ".*" ersetzt, bevor der Ausdruck durch
   * {@link Pattern#compile(String)} kompiliert wird.<br/>
   * Beispiele:
   * <ul>
   * <li>5.100.* -&gt; 5\.100\..*</li>
   * <li>*.10.* -&gt; .*\.10\..*</li>
   * </ul>
   * @param regExpList
   * @param kost
   * @param emptyListMatches
   * @return
   * @see String#matches(String)()
   * @see #modifyRegExp(String)
   */
  public static boolean match(List<String> regExpList, String kost, boolean emptyListMatches)
  {
    if (CollectionUtils.isNotEmpty(regExpList) == true) {
      for (String str : regExpList) {
        String regExp = modifyRegExp(str);
        if (kost.matches(regExp) == true) {
          return true;
        }
      }
      return false;
    } else {
      // List is empty:
      return emptyListMatches;
    }
  }

  /**
   * Alle Punkte werden gequoted und alle * durch ".*" ersetzt. Ausnahme: Der String beginnt mit einem einfachen Hochkomma, dann werden
   * keine Ersetzungen durchgeführt, sondern lediglich das Hochkomma selbst entfernt.
   * @param regExp
   */
  public static String modifyRegExp(String regExp)
  {
    if (regExp == null) {
      return null;
    }
    if (regExp.startsWith("'") == true) {
      return regExp.substring(1);
    }
    String str = regExp.replace(".", "\\.").replace("*", ".*");
    return str;
  }
}
