/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2012 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.plugins.todo;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ObjectUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.task.TaskTree;
import org.projectforge.user.GroupDO;
import org.projectforge.user.UserGroupCache;
import org.projectforge.web.calendar.DateTimeFormatter;
import org.projectforge.web.core.PriorityFormatter;
import org.projectforge.web.task.TaskPropertyColumn;
import org.projectforge.web.user.UserFormatter;
import org.projectforge.web.user.UserPrefListPage;
import org.projectforge.web.user.UserPropertyColumn;
import org.projectforge.web.wicket.AbstractListPage;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.DetachableDOModel;
import org.projectforge.web.wicket.IListPageColumnsCreator;
import org.projectforge.web.wicket.ListPage;
import org.projectforge.web.wicket.ListSelectActionPanel;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;

@ListPage(editPage = ToDoEditPage.class)
public class ToDoListPage extends AbstractListPage<ToDoListForm, ToDoDao, ToDoDO> implements IListPageColumnsCreator<ToDoDO>
{
  private static final long serialVersionUID = 3232839536537741949L;

  @SpringBean(name = "toDoDao")
  private ToDoDao toDoDao;

  @SpringBean(name = "priorityFormatter")
  private PriorityFormatter priorityFormatter;

  @SpringBean(name = "taskTree")
  private TaskTree taskTree;

  @SpringBean(name = "userFormatter")
  private UserFormatter userFormatter;

  @SpringBean(name = "userGroupCache")
  private UserGroupCache userGroupCache;

  public ToDoListPage(final PageParameters parameters)
  {
    super(parameters, "plugins.todo");
  }

  @SuppressWarnings("serial")
  public List<IColumn<ToDoDO, String>> createColumns(final WebPage returnToPage, final boolean sortable)
  {
    final List<IColumn<ToDoDO, String>> columns = new ArrayList<IColumn<ToDoDO, String>>();
    final CellItemListener<ToDoDO> cellItemListener = new CellItemListener<ToDoDO>() {
      public void populateItem(final Item<ICellPopulator<ToDoDO>> item, final String componentId, final IModel<ToDoDO> rowModel)
      {
        final ToDoDO toDo = rowModel.getObject();
        final StringBuffer cssStyle = getCssStyle(toDo.getId(), toDo.isDeleted());
        if (toDo.isRecent() == true && ObjectUtils.equals(getUserId(), toDo.getAssigneeId()) == true) {
          cssStyle.append("color:red;");
        }
        if (cssStyle.length() > 0) {
          item.add(AttributeModifier.append("style", new Model<String>(cssStyle.toString())));
        }
      }
    };

    columns.add(new CellItemListenerPropertyColumn<ToDoDO>(new Model<String>(getString("created")), getSortable("created", sortable),
        "created", cellItemListener) {
      @SuppressWarnings({ "unchecked", "rawtypes"})
      @Override
      public void populateItem(final Item item, final String componentId, final IModel rowModel)
      {
        final ToDoDO toDo = (ToDoDO) rowModel.getObject();
        item.add(new ListSelectActionPanel(componentId, rowModel, ToDoEditPage.class, toDo.getId(), returnToPage, DateTimeFormatter
            .instance().getFormattedDateTime(toDo.getCreated())));
        addRowClick(item);
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<ToDoDO>(getString("modified"), getSortable("lastUpdate", sortable), "lastUpdate",
        cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<ToDoDO>(new Model<String>(getString("plugins.todo.subject")), getSortable("subject",
        sortable), "subject", cellItemListener));
    columns.add(new UserPropertyColumn<ToDoDO>(getString("plugins.todo.assignee"), getSortable("assigneeId", sortable), "assignee",
        cellItemListener).withUserFormatter(userFormatter));
    columns.add(new UserPropertyColumn<ToDoDO>(getString("plugins.todo.reporter"), getSortable("reporterId", sortable), "reporter",
        cellItemListener).withUserFormatter(userFormatter));
    columns.add(new CellItemListenerPropertyColumn<ToDoDO>(getString("dueDate"), getSortable("dueDate", sortable), "dueDate",
        cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<ToDoDO>(new Model<String>(getString("plugins.todo.status")), getSortable("status",
        sortable), "status", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<ToDoDO>(new Model<String>(getString("priority")), getSortable("priority", sortable),
        "priority", cellItemListener) {
      @Override
      public void populateItem(final Item<ICellPopulator<ToDoDO>> item, final String componentId, final IModel<ToDoDO> rowModel)
      {
        final ToDoDO todo = rowModel.getObject();
        final String formattedPriority = priorityFormatter.getFormattedPriority(todo.getPriority());
        final Label label = new Label(componentId, new Model<String>(formattedPriority));
        label.setEscapeModelStrings(false);
        item.add(label);
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<ToDoDO>(new Model<String>(getString("plugins.todo.type")),
        getSortable("type", sortable), "type", cellItemListener));
    columns.add(new TaskPropertyColumn<ToDoDO>(getString("task"), getSortable("task.title", sortable), "task", cellItemListener)
        .withTaskTree(taskTree));
    columns.add(new CellItemListenerPropertyColumn<ToDoDO>(new Model<String>(getString("group")), null, "group", cellItemListener) {
      @Override
      public void populateItem(final Item<ICellPopulator<ToDoDO>> item, final String componentId, final IModel<ToDoDO> rowModel)
      {
        final ToDoDO projektDO = rowModel.getObject();
        String groupName = "";
        if (projektDO.getGroup() != null) {
          final GroupDO group = userGroupCache.getGroup(projektDO.getGroupId());
          if (group != null) {
            groupName = group.getName();
          }
        }
        final Label label = new Label(componentId, groupName);
        item.add(label);
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<ToDoDO>(new Model<String>(getString("description")),
        getSortable("description", sortable), "description", cellItemListener));
    return columns;
  }

  @Override
  protected void init()
  {
    dataTable = createDataTable(createColumns(this, true), "lastUpdate", SortOrder.DESCENDING);
    form.add(dataTable);
    final BookmarkablePageLink<Void> addTemplatesLink = UserPrefListPage.createLink("link", ToDoPlugin.USER_PREF_AREA);
    final ContentMenuEntryPanel menuEntry = new ContentMenuEntryPanel(getNewContentMenuChildId(), addTemplatesLink, getString("templates"));
    addContentMenuEntry(menuEntry);
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListPage#select(java.lang.String, java.lang.Object)
   */
  @Override
  public void select(final String property, final Object selectedValue)
  {
    if ("taskId".equals(property) == true) {
      form.getSearchFilter().setTaskId((Integer) selectedValue);
      refresh();
    } else if ("reporterId".equals(property) == true) {
      form.getSearchFilter().setReporterId((Integer) selectedValue);
      refresh();
    } else if ("assigneeId".equals(property) == true) {
      form.getSearchFilter().setAssigneeId((Integer) selectedValue);
      refresh();
    } else {
      super.select(property, selectedValue);
    }
  }

  /**
   * 
   * @see org.projectforge.web.fibu.ISelectCallerPage#unselect(java.lang.String)
   */
  @Override
  public void unselect(final String property)
  {
    if ("taskId".equals(property) == true) {
      form.getSearchFilter().setTaskId(null);
      refresh();
    } else if ("reporterId".equals(property) == true) {
      form.getSearchFilter().setReporterId(null);
      refresh();
    } else if ("assigneeId".equals(property) == true) {
      form.getSearchFilter().setAssigneeId(null);
      refresh();
    } else {
      super.unselect(property);
    }
  }

  @Override
  protected ToDoListForm newListForm(final AbstractListPage< ? , ? , ? > parentPage)
  {
    return new ToDoListForm(this);
  }

  @Override
  protected ToDoDao getBaseDao()
  {
    return toDoDao;
  }

  @Override
  protected IModel<ToDoDO> getModel(final ToDoDO object)
  {
    return new DetachableDOModel<ToDoDO, ToDoDao>(object, getBaseDao());
  }

  protected ToDoDao getToDoDao()
  {
    return toDoDao;
  }
}
