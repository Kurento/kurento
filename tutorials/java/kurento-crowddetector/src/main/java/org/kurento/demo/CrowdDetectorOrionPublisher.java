/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.kurento.demo;

import static com.google.common.collect.Lists.newArrayListWithCapacity;

import java.util.Collection;
import java.util.List;

import org.kurento.module.crowddetector.CrowdDetectorDirectionEvent;
import org.kurento.module.crowddetector.CrowdDetectorFluidityEvent;
import org.kurento.module.crowddetector.CrowdDetectorOccupancyEvent;
import org.kurento.module.crowddetector.RegionOfInterest;
import org.kurento.orion.OrionConnector;
import org.kurento.orion.entities.OrionAttribute;
import org.kurento.orion.entities.OrionContextElement;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Ivan Gracia (igracia@kurento.com)
 *
 */
public class CrowdDetectorOrionPublisher {

  public static final String LEVEL_ATTR = "Level";
  public static final String PERCENTAGE_ATTR = "Percentage";
  public static final String DIRECTION_ATTR = "Direction";

  @Autowired
  private OrionConnector orionConnector;

  public void publishEvent(CrowdDetectorDirectionEvent event) {
    OrionContextElement contextElement = directionEventToContextElement(event);
    this.orionConnector.updateContextElements(contextElement);
  }

  public void publishEvent(CrowdDetectorFluidityEvent event) {
    OrionContextElement contextElement = fluidityEventToContextElement(event);
    this.orionConnector.updateContextElements(contextElement);
  }

  public void publishEvent(CrowdDetectorOccupancyEvent event) {
    OrionContextElement contextElement = occupancyEventToContextElement(event);
    this.orionConnector.updateContextElements(contextElement);
  }

  public void registerRoisInOrion(Collection<RegionOfInterest> rois) {

    List<OrionContextElement> events = newArrayListWithCapacity(rois.size());

    for (RegionOfInterest roi : rois) {
      events.add(occupancyEventToContextElement(roi.getId()));
      events.add(directionEventToContextElement(roi.getId()));
      events.add(fluidityEventToContextElement(roi.getId()));
    }

    this.orionConnector
        .registerContextElements(events.toArray(new OrionContextElement[events.size()]));
  }

  private static OrionContextElement occupancyEventToContextElement(String roiId) {
    OrionContextElement contextElement = new OrionContextElement();

    contextElement.setId(roiId);
    contextElement.setPattern(false);
    contextElement.setType(CrowdDetectorOccupancyEvent.class.getSimpleName());

    OrionAttribute<Integer> attrLevel =
        new OrionAttribute<>(LEVEL_ATTR, Integer.class.getSimpleName(), Integer.valueOf(0));
    OrionAttribute<Float> attrPercentage =
        new OrionAttribute<>(PERCENTAGE_ATTR, Float.class.getSimpleName(), 0F);
    contextElement.getAttributes().add(attrLevel);
    contextElement.getAttributes().add(attrPercentage);

    return contextElement;
  }

  private static OrionContextElement occupancyEventToContextElement(
      CrowdDetectorOccupancyEvent event) {
    OrionContextElement contextElement = new OrionContextElement();

    contextElement.setId(event.getRoiID());
    contextElement.setType(event.getClass().getSimpleName());

    OrionAttribute<Integer> attrLevel =
        new OrionAttribute<>(LEVEL_ATTR, Integer.class.getSimpleName(), event.getOccupancyLevel());
    OrionAttribute<Float> attrPercentage = new OrionAttribute<>(PERCENTAGE_ATTR,
        Float.class.getSimpleName(), event.getOccupancyPercentage());
    contextElement.getAttributes().add(attrLevel);
    contextElement.getAttributes().add(attrPercentage);

    return contextElement;
  }

  private static OrionContextElement fluidityEventToContextElement(String roiId) {
    OrionContextElement contextElement = new OrionContextElement();

    contextElement.setId(roiId);
    contextElement.setPattern(false);
    contextElement.setType(CrowdDetectorFluidityEvent.class.getSimpleName());

    OrionAttribute<Integer> attrLevel =
        new OrionAttribute<>(LEVEL_ATTR, Integer.class.getSimpleName(), Integer.valueOf(0));
    OrionAttribute<Float> attrPercentage =
        new OrionAttribute<>(PERCENTAGE_ATTR, Float.class.getSimpleName(), 0F);
    contextElement.getAttributes().add(attrLevel);
    contextElement.getAttributes().add(attrPercentage);

    return contextElement;
  }

  private static OrionContextElement fluidityEventToContextElement(
      CrowdDetectorFluidityEvent event) {
    OrionContextElement contextElement = new OrionContextElement();

    contextElement.setId(event.getRoiID());
    contextElement.setType(event.getClass().getSimpleName());

    OrionAttribute<Integer> attrLevel =
        new OrionAttribute<>(LEVEL_ATTR, Integer.class.getSimpleName(), event.getFluidityLevel());
    OrionAttribute<Float> attrPercentage = new OrionAttribute<>(PERCENTAGE_ATTR,
        Float.class.getSimpleName(), event.getFluidityPercentage());
    contextElement.getAttributes().add(attrLevel);
    contextElement.getAttributes().add(attrPercentage);

    return contextElement;
  }

  private static OrionContextElement directionEventToContextElement(String roiId) {
    OrionContextElement contextElement = new OrionContextElement();

    contextElement.setId(roiId);
    contextElement.setPattern(false);
    contextElement.setType(CrowdDetectorDirectionEvent.class.getSimpleName());

    OrionAttribute<Float> directionAttribute =
        new OrionAttribute<>(DIRECTION_ATTR, Float.class.getSimpleName(), Float.valueOf(0));

    contextElement.getAttributes().add(directionAttribute);
    return contextElement;
  }

  private static OrionContextElement directionEventToContextElement(
      CrowdDetectorDirectionEvent event) {
    OrionContextElement contextElement = new OrionContextElement();

    contextElement.setId(event.getRoiID());
    contextElement.setType(event.getClass().getSimpleName());

    OrionAttribute<Float> directionAttribute = new OrionAttribute<>(DIRECTION_ATTR,
        Float.class.getSimpleName(), event.getDirectionAngle());

    contextElement.getAttributes().add(directionAttribute);
    return contextElement;
  }

}
