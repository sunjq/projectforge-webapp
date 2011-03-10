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

package org.projectforge.web.access;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.PageParameters;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.access.AccessDao;
import org.projectforge.access.AccessEntryDO;
import org.projectforge.access.GroupTaskAccessDO;
import org.projectforge.task.TaskDO;
import org.projectforge.web.task.TaskFormatter;
import org.projectforge.web.wicket.AbstractListPage;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.DetachableDOModel;
import org.projectforge.web.wicket.IListPageColumnsCreator;
import org.projectforge.web.wicket.ListPage;
import org.projectforge.web.wicket.ListSelectActionPanel;
import org.projectforge.web.wicket.WebConstants;
import org.projectforge.web.wicket.WicketLocalizerAndUrlBuilder;
import org.projectforge.web.wicket.components.SingleImagePanel;

@ListPage(editPage = AccessEditPage.class)
public class AccessListPage extends AbstractListPage<AccessListForm, AccessDao, GroupTaskAccessDO> implements
    IListPageColumnsCreator<GroupTaskAccessDO>
{
  /**
   * Key for pre-setting the task id.
   */
  public static final String PARAMETER_KEY_TASK_ID = "taskId";

  private static final long serialVersionUID = 7017404582337466883L;

  @SpringBean(name = "accessDao")
  private AccessDao accessDao;

  @SpringBean(name = "taskFormatter")
  private TaskFormatter taskFormatter;

  public AccessListPage(PageParameters parameters)
  {
    super(parameters, "access");
    if (parameters.containsKey(PARAMETER_KEY_TASK_ID) == true) {
      final Integer id = parameters.getAsInteger(PARAMETER_KEY_TASK_ID);
      form.getSearchFilter().setTaskId(id);
    }
  }

  @SuppressWarnings("serial")
  @Override
  public List<IColumn<GroupTaskAccessDO>> createColumns(final WebPage returnToPage, final boolean sortable)
  {
    final List<IColumn<GroupTaskAccessDO>> columns = new ArrayList<IColumn<GroupTaskAccessDO>>();
    final CellItemListener<GroupTaskAccessDO> cellItemListener = new CellItemListener<GroupTaskAccessDO>() {
      public void populateItem(Item<ICellPopulator<GroupTaskAccessDO>> item, String componentId, IModel<GroupTaskAccessDO> rowModel)
      {
        final GroupTaskAccessDO acces = rowModel.getObject();
        final StringBuffer cssStyle = getCssStyle(acces.getId(), acces.isDeleted());
        if (cssStyle.length() > 0) {
          item.add(new AttributeModifier("style", true, new Model<String>(cssStyle.toString())));
        }
      }
    };
    columns.add(new CellItemListenerPropertyColumn<GroupTaskAccessDO>(new Model<String>(getString("task")), getSortable("task.title",
        sortable), "task.title", cellItemListener) {
      @SuppressWarnings("unchecked")
      @Override
      public void populateItem(final Item item, final String componentId, final IModel rowModel)
      {
        final GroupTaskAccessDO access = (GroupTaskAccessDO) rowModel.getObject();
        final TaskDO task = access.getTask();
        final StringBuffer buf = new StringBuffer();
        taskFormatter.appendFormattedTask(buf, new WicketLocalizerAndUrlBuilder(getResponse()), task, false, true, false);
        final Label formattedTaskLabel = new Label(ListSelectActionPanel.LABEL_ID, buf.toString());
        formattedTaskLabel.setEscapeModelStrings(false);
        item.add(new ListSelectActionPanel(componentId, rowModel, AccessEditPage.class, access.getId(), returnToPage, formattedTaskLabel));
        addRowClick(item);
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<GroupTaskAccessDO>(new Model<String>(getString("group")), getSortable("group.name",
        sortable), "group.name", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<GroupTaskAccessDO>(new Model<String>(getString("recursive")), getSortable("recursive",
        sortable), "recursive", cellItemListener) {
      @Override
      public void populateItem(Item<ICellPopulator<GroupTaskAccessDO>> item, String componentId, IModel<GroupTaskAccessDO> rowModel)
      {
        final GroupTaskAccessDO access = (GroupTaskAccessDO) rowModel.getObject();
        if (access.isRecursive() == true) {
          item.add(SingleImagePanel.createPresizedImage(componentId, WebConstants.IMAGE_ACCEPT));
        } else {
          item.add(createInvisibleDummyComponent(componentId));
        }
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<GroupTaskAccessDO>(new Model<String>(getString("access.type")), null, "accessEntries",
        cellItemListener) {
      @Override
      public void populateItem(Item<ICellPopulator<GroupTaskAccessDO>> item, String componentId, IModel<GroupTaskAccessDO> rowModel)
      {
        final int rowIndex = ((Item< ? >) item.findParent(Item.class)).getIndex();
        final GroupTaskAccessDO access = (GroupTaskAccessDO) rowModel.getObject();
        final List<AccessEntryDO> accessEntries = access.getOrderedEntries();
        final AccessTablePanel accessTablePanel = new AccessTablePanel(componentId, accessEntries);
        if (rowIndex == 0) {
          accessTablePanel.setDrawHeader(true);
        }
        item.add(accessTablePanel);
        accessTablePanel.init();
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<GroupTaskAccessDO>(getString("description"), getSortable("description", sortable),
        "description", cellItemListener) {
      @Override
      public void populateItem(Item<ICellPopulator<GroupTaskAccessDO>> item, String componentId, IModel<GroupTaskAccessDO> rowModel)
      {
        final GroupTaskAccessDO access = rowModel.getObject();
        final Label label = new Label(componentId, StringUtils.abbreviate(access.getDescription(), 100));
        cellItemListener.populateItem(item, componentId, rowModel);
        item.add(label);
      }
    });
    return columns;
  }

  @Override
  protected void init()
  {
    dataTable = createDataTable(createColumns(this, true), "group.name", true);
    form.add(dataTable);
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
    } else if ("groupId".equals(property) == true) {
      form.getSearchFilter().setGroupId((Integer) selectedValue);
      refresh();
    } else if ("userId".equals(property) == true) {
      form.getSearchFilter().setUserId((Integer) selectedValue);
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
    } else if ("groupId".equals(property) == true) {
      form.getSearchFilter().setGroupId(null);
      refresh();
    } else if ("userId".equals(property) == true) {
      form.getSearchFilter().setUserId(null);
      refresh();
    } else {
      super.unselect(property);
    }
  }

  @Override
  protected AccessListForm newListForm(AbstractListPage< ? , ? , ? > parentPage)
  {
    return new AccessListForm(this);
  }

  @Override
  protected AccessDao getBaseDao()
  {
    return accessDao;
  }

  @Override
  protected IModel<GroupTaskAccessDO> getModel(GroupTaskAccessDO object)
  {
    return new DetachableDOModel<GroupTaskAccessDO, AccessDao>(object, getBaseDao());
  }

  protected AccessDao getAccessDao()
  {
    return accessDao;
  }
}
