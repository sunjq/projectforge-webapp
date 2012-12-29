/**
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package net.ftlines.wicket.fullcalendar.callback;

import net.ftlines.wicket.fullcalendar.CalendarResponse;
import net.ftlines.wicket.fullcalendar.Event;
import net.ftlines.wicket.fullcalendar.EventSource;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.Request;

public abstract class EventDroppedCallback extends AbstractAjaxCallbackWithClientsideRevert implements CallbackWithHandler
{
  @Override
  protected String configureCallbackScript(final String script, final String urlTail)
  {
    return script.replace(urlTail, "&eventId=\"+event.id+\"&sourceId=\"+event.source.data."
        + EventSource.Const.UUID
        + "+\"&dayDelta=\"+dayDelta+\"&minuteDelta=\"+minuteDelta+\"&allDay=\"+allDay+\"");
  }

  @Override
  public String getHandlerScript()
  {
    return "function(event, dayDelta, minuteDelta, allDay, revertFunc) { " + getCallbackScript() + "}";
  }

  @Override
  protected boolean onEvent(final AjaxRequestTarget target)
  {
    final Request r = getCalendar().getRequest();
    final String eventId = r.getRequestParameters().getParameterValue("eventId").toString();
    final String sourceId = r.getRequestParameters().getParameterValue("sourceId").toString();

    final EventSource source = getCalendar().getEventManager().getEventSource(sourceId);
    final Event event = source.getEventProvider().getEventForId(eventId);

    final int dayDelta = r.getRequestParameters().getParameterValue("dayDelta").toInt();
    final int minuteDelta = r.getRequestParameters().getParameterValue("minuteDelta").toInt();
    final boolean allDay = r.getRequestParameters().getParameterValue("allDay").toBoolean();

    return onEventDropped(new DroppedEvent(source, event, dayDelta, minuteDelta, allDay), new CalendarResponse(getCalendar(), target));
  }

  protected abstract boolean onEventDropped(DroppedEvent event, CalendarResponse response);

  @Override
  protected String getRevertScript()
  {
    return "revertFunc();";
  }

}
